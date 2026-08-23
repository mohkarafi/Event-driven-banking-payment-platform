package karafi.paymentservice.service;

import feign.FeignException;
import karafi.paymentservice.client.AccountClient;
import karafi.paymentservice.dto.*;
import karafi.paymentservice.entity.Payment;
import karafi.paymentservice.entity.PaymentStatus;
import karafi.paymentservice.exception.*;
import karafi.paymentservice.kafka.PaymentCompletedEvent;
import karafi.paymentservice.kafka.PaymentEventProducer;
import karafi.paymentservice.kafka.PaymentFailedEvent;
import karafi.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentRepository paymentRepository;
    private final AccountClient accountClient;
    private final PaymentEventProducer eventProducer;

    @Override
    @Transactional
    public ApiResponse<PaymentResponse> createPayment(CreatePaymentRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidPaymentException("Idempotency-Key header is required");
        }

        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay detected for key={}, returning existing payment reference={} ", idempotencyKey, existing.get().getPaymentReference());
            return ApiResponse.of(200, "Payment already exists for this idempotency key.", toResponse(existing.get()));
        }

        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidPaymentException("sourceAccountNumber and destinationAccountNumber must be different");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("amount must be greater than 0");
        }

        Payment payment = Payment.builder()
                .paymentReference(generatePaymentReference())
                .idempotencyKey(idempotencyKey)
                .sourceAccountNumber(request.sourceAccountNumber())
                .destinationAccountNumber(request.destinationAccountNumber())
                .amount(request.amount())
                .currency(request.currency())
                .description(request.description())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

  /*   try {
        payment = paymentRepository.save(payment);
    } catch (DataIntegrityViolationException e) {
        Payment winner = paymentRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new PaymentProcessingException("Concurrent payment creation c
                        log.warn("Concurrent create detected for idempotencyKey={}, returning winning payment re
                                idempotencyKey, winner.getPaymentReference());
        return ApiResponse.of(200, "Payment already exists for this idempotency key.", toRespons
    }*/

        log.info("Payment created: reference={}, source={}, destination={}, amount={}",
                payment.getPaymentReference(), payment.getSourceAccountNumber(),
                payment.getDestinationAccountNumber(), payment.getAmount());
        return ApiResponse.of(201, "Payment created successfully.", toResponse(payment));
    }

    @Override
    public ApiResponse<PaymentResponse> getPayment(Long paymentId) {
        return null;
    }

    @Override
    public ApiResponse<PaymentResponse> getPaymentByReference(String paymentReference) {
        return null;
    }

// ------------------------------------------------------------------
    // Execute
    // ------------------------------------------------------------------

    @Override
    public ApiResponse<PaymentResponse> executePayment(Long paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        assertCanStartProcessing(payment);

        transition(payment, PaymentStatus.PROCESSING);

        AccountDto source;
        AccountDto destination;
        try {
            source = accountClient.getAccountByNumber(payment.getSourceAccountNumber());
            destination = accountClient.getAccountByNumber(payment.getDestinationAccountNumber());
        } catch (FeignException.NotFound e) {
            return fail(payment, "Source or destination account not found");
        }

        if (source.status() != AccountStatus.ACTIVE) {
            return fail(payment, "Source account is not active");
        }
        if (destination.status() != AccountStatus.ACTIVE) {
            return fail(payment, "Destination account is not active");
        }
        if (source.balance().compareTo(payment.getAmount()) < 0) {
            return fail(payment, "Insufficient balance on source account");
        }

        // Step 1: debit the source account.
        try {
            accountClient.withdraw(payment.getSourceAccountNumber(), AmountRequest.builder().amount(payment.getAmount()).currency(payment.getCurrency()).build());
        } catch (Exception e) {
            log.error("Withdraw failed for payment reference={}", payment.getPaymentReference(), e);
            return fail(payment, "Withdraw failed on source account");
        }

        try {
            accountClient.deposit(payment.getDestinationAccountNumber(), AmountRequest.builder().amount(payment.getAmount()).currency(payment.getCurrency()).build());
        } catch (Exception depositEx) {
            log.error("Deposit failed after successful withdraw for payment reference={}. Attempting compensation.",
                    payment.getPaymentReference(), depositEx);
            try {
                accountClient.deposit(payment.getSourceAccountNumber(), AmountRequest.builder().amount(payment.getAmount()).currency(payment.getCurrency()).build());
                log.warn("Compensation succeeded: reversed withdraw for payment reference={}",
                        payment.getPaymentReference());
            } catch (Exception compensationEx) {
                log.error("CRITICAL: compensation FAILED for payment reference={}. Source account {} may be "
                                + "short by {} {}. Manual reconciliation required.",
                        payment.getPaymentReference(), payment.getSourceAccountNumber(),
                        payment.getAmount(), payment.getCurrency(), compensationEx);
            }
            return fail(payment, "Deposit failed on destination account");
        }

        transition(payment, PaymentStatus.COMPLETED);
        log.info("Payment completed: reference={}", payment.getPaymentReference());

        eventProducer.publishPaymentCompleted(new PaymentCompletedEvent(
                payment.getPaymentReference(),
                payment.getSourceAccountNumber(),
                payment.getDestinationAccountNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                LocalDateTime.now()
        ));

        return ApiResponse.of(200, "Payment executed successfully.", toResponse(payment));
    }

    private ApiResponse<PaymentResponse> fail(Payment payment, String reason) {
        transition(payment, PaymentStatus.FAILED);
        log.warn("Payment failed: reference={}, reason={}", payment.getPaymentReference(), reason);

        eventProducer.publishPaymentFailed(new PaymentFailedEvent(
                payment.getPaymentReference(),
                payment.getSourceAccountNumber(),
                payment.getDestinationAccountNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                reason,
                LocalDateTime.now()
        ));

        return ApiResponse.of(200, "Payment failed: " + reason, toResponse(payment));
    }

    private void assertCanStartProcessing(Payment payment) {
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentAlreadyCompletedException(
                    "Payment " + payment.getPaymentReference() + " is already completed");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new PaymentAlreadyCancelledException(
                    "Payment " + payment.getPaymentReference() + " is cancelled");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(
                    "Payment " + payment.getPaymentReference() + " cannot be executed from state "
                            + payment.getStatus());
        }
    }

    @Override
    public ApiResponse<PaymentResponse> cancelPayment(Long paymentId) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PaymentResponse>> getPaymentsByAccount(String accountNumber) {
        List<PaymentResponse> payments = paymentRepository
                .findBySourceAccountNumberOrDestinationAccountNumber(accountNumber, accountNumber)
                .stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.of(200, "Payments retrieved successfully.", payments);
    }





    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private Payment getPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
    }

    @Transactional
    protected void transition(Payment payment, PaymentStatus target) {
        if (!payment.getStatus().canTransitionTo(target)) {
            throw new InvalidPaymentStateException(
                    "Cannot transition payment " + payment.getPaymentReference()
                            + " from " + payment.getStatus() + " to " + target);
        }
        payment.setStatus(target);
        paymentRepository.save(payment);
    }

    private String generatePaymentReference() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "PAY-" + LocalDate.now().format(DATE_FORMAT) + "-" + randomAlphaNumeric(4);
            if (!paymentRepository.existsByPaymentReference(candidate)) {
                return candidate;
            }
        }
        throw new PaymentProcessingException("Unable to generate a unique payment reference");
    }


    private String randomAlphaNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }


    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getSourceAccountNumber(),
                payment.getDestinationAccountNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
