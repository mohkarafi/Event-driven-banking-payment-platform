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
import karafi.paymentservice.mapper.PaymentMapper;
import karafi.paymentservice.repository.PaymentRepository;
import karafi.paymentservice.util.PaymentUtils;
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

import static java.util.stream.Collectors.toList;


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
    private final PaymentUtils paymentUtils;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public ApiResponse<PaymentResponse> createPayment(CreatePaymentRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidPaymentException("Idempotency-Key header is required");
        }

        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay detected for key={}, returning existing payment reference={} ", idempotencyKey, existing.get().getPaymentReference());
            return ApiResponse.of(200, "Payment already exists for this idempotency key.", paymentMapper.toResponse(existing.get()));
        }

        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidPaymentException("sourceAccountNumber and destinationAccountNumber must be different");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("amount must be greater than 0");
        }

        Payment payment = Payment.builder()
                .paymentReference(paymentUtils.generatePaymentReference())
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
        return ApiResponse.of(201, "Payment created successfully.", paymentMapper.toResponse(payment));
    }

// ------------------------------------------------------------------
    // Execute
    // ------------------------------------------------------------------

    @Override
    public ApiResponse<PaymentResponse> executePayment(Long paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        assertCanStartProcessing(payment);

        transition(payment, PaymentStatus.PROCESSING);

        try {
            return doExecute(payment);
        } catch (Exception e) {
            log.error("Unexpected error during payment execution reference={}", payment.getPaymentReference(), e);
            return fail(payment, "Unexpected error during payment processing");
        }
    }

    private ApiResponse<PaymentResponse> doExecute(Payment payment) {
        AccountDto source;
        AccountDto destination;
        try {
            source = accountClient.getAccountByNumber(payment.getSourceAccountNumber()).data();
            destination = accountClient.getAccountByNumber(payment.getDestinationAccountNumber()).data();
        } catch (FeignException.NotFound e) {
            return fail(payment, "Source or destination account not found");
        }

        validateAccountData(source, payment.getSourceAccountNumber());
        validateAccountData(destination, payment.getDestinationAccountNumber());

        if (source.status() != AccountStatus.ACTIVE) {
            return fail(payment, "Source account is not active");
        }
        if (destination.status() != AccountStatus.ACTIVE) {
            return fail(payment, "Destination account is not active");
        }
        if (source.balance().compareTo(payment.getAmount()) < 0) {
            return fail(payment, "Insufficient balance on source account");
        }

        try {
            accountClient.withdraw(payment.getSourceAccountNumber(),
                    AmountRequest.builder().amount(payment.getAmount()).currency(payment.getCurrency()).build());
        } catch (Exception e) {
            log.error("Withdraw failed for payment reference={}", payment.getPaymentReference(), e);
            return fail(payment, "Withdraw failed on source account");
        }

        try {
            accountClient.deposit(payment.getDestinationAccountNumber(),
                    AmountRequest.builder().amount(payment.getAmount()).currency(payment.getCurrency()).build());
        } catch (Exception depositEx) {
            log.error("Deposit failed after successful withdraw for payment reference={}. Attempting compensation.",
                    payment.getPaymentReference(), depositEx);
            try {
                accountClient.deposit(payment.getSourceAccountNumber(),
                        AmountRequest.builder().amount(payment.getAmount()).currency(payment.getCurrency()).build());
                log.warn("Compensation succeeded: reversed withdraw for payment reference={}", payment.getPaymentReference());
            } catch (Exception compensationEx) {
                log.error("CRITICAL: compensation FAILED for payment reference={}. Source account {} may be short by {} {}. Manual reconciliation required.",
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

        return ApiResponse.of(200, "Payment executed successfully.", paymentMapper.toResponse(payment));
    }

    private void validateAccountData(AccountDto account, String accountNumber) {
        if (account == null || account.balance() == null || account.status() == null) {
            throw new PaymentProcessingException("Incomplete account data received from account-service for " + accountNumber);
        }
    }

    // ------------------------------------------------------------------
    // Cancel
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ApiResponse<PaymentResponse> cancelPayment(Long paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentAlreadyCompletedException(
                    "Payment " + payment.getPaymentReference()
                            + " is already completed and cannot be cancelled; use a refund instead");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new PaymentAlreadyCancelledException(
                    "Payment " + payment.getPaymentReference() + " is already cancelled");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(
                    "Payment " + payment.getPaymentReference() + " cannot be cancelled from state "
                            + payment.getStatus());
        }

        transition(payment, PaymentStatus.CANCELLED);
        log.info("Payment cancelled: reference={}", payment.getPaymentReference());

        return ApiResponse.of(200, "Payment cancelled successfully.", paymentMapper.toResponse(payment));
    }



    // ----------------------------
    // fail
    // -----------------------------
    private ApiResponse<PaymentResponse> fail(Payment payment, String reason) {
        transition(payment, PaymentStatus.FAILED);

        log.warn("Payment failed: reference={}, reason={}", payment.getPaymentReference(), reason);
        eventProducer.publishPaymentFailed(PaymentFailedEvent.builder()
                        .paymentReference(payment.getPaymentReference())
                        .reason(reason)
                        .sourceAccountNumber(payment.getSourceAccountNumber())
                        .currency(payment.getCurrency())
                        .amount(payment.getAmount())
                        .destinationAccountNumber(payment.getDestinationAccountNumber())
                        .failedAt(LocalDateTime.now())
                .build());
        return ApiResponse.of(402, "Payment failed: " + reason, paymentMapper.toResponse(payment));
    }

    // --------------------------------------
    // Asserts
    // ------------------------------------------
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

    // --------------------------------------
    // Get Payment
    // ------------------------------------------


    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PaymentResponse> getPayment(Long paymentId) {
        return ApiResponse.of(200, "Payment retrieved successfully.", paymentMapper.toResponse(getPaymentOrThrow(paymentId)));
    }


    // --------------------------------------
    //  Get Payment By reference
    // ------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PaymentResponse> getPaymentByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentReference));
        return ApiResponse.of(200, "Payment retrieved successfully.", paymentMapper.toResponse(payment));
    }

    // --------------------------------------
    //  Get Payment By Account
    // ------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PaymentResponse>> getPaymentsByAccount(String accountNumber) {
        List<PaymentResponse> payments = paymentRepository
                .findBySourceAccountNumberOrDestinationAccountNumber(accountNumber, accountNumber)
                .stream()
                .map(paymentMapper::toResponse)
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




}
