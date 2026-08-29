package karafi.transactionservice.service;


import karafi.transactionservice.dto.PaymentCompletedEvent;
import karafi.transactionservice.dto.TransactionResponse;
import karafi.transactionservice.entity.Transaction;
import karafi.transactionservice.entity.TransactionStatus;
import karafi.transactionservice.entity.TransactionType;
import karafi.transactionservice.exception.TransactionNotFoundException;
import karafi.transactionservice.mapper.TransactionMapper;
import karafi.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public void createFromPaymentCompleted(PaymentCompletedEvent event) {
        if (event.paymentReference() == null || event.paymentReference().isBlank()) {
            log.error("Rejected PaymentCompletedEvent with missing paymentReference: {}", event);
            return;
        }
        if (event.amount() == null || event.sourceAccountNumber() == null || event.destinationAccountNumber() == null) {
            log.error("Rejected incomplete PaymentCompletedEvent for paymentReference={}", event.paymentReference());
            return;
        }
        if (transactionRepository.existsByPaymentReference(event.paymentReference())) {
            log.info("Transaction already exists for paymentReference={}, ignoring duplicate event",
                    event.paymentReference());
            return;
        }

        Transaction transaction = Transaction.builder()
                .transactionReference(generateTransactionReference())
                .paymentReference(event.paymentReference())
                .sourceAccountNumber(event.sourceAccountNumber())
                .destinationAccountNumber(event.destinationAccountNumber())
                .amount(event.amount())
                .currency(event.currency())
                .status(TransactionStatus.COMPLETED)
                .transactionType(TransactionType.TRANSFER)
                .completedAt(event.completedAt() != null ? event.completedAt() : LocalDateTime.now())
                .build();

        try {
            transaction = transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent insert detected for paymentReference={}, transaction already recorded",
                    event.paymentReference());
            return;
        }

        log.info("Transaction recorded: transactionReference={}, paymentReference={}, {} {} from {} to {}",
                transaction.getTransactionReference(), transaction.getPaymentReference(),
                transaction.getAmount(), transaction.getCurrency(),
                transaction.getSourceAccountNumber(), transaction.getDestinationAccountNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id) {
        return transactionMapper.toResponse(transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String transactionReference) {
        return transactionMapper.toResponse(transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found: " + transactionReference)));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getByPaymentReference(String paymentReference) {
        return transactionMapper.toResponse(transactionRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "No transaction recorded for paymentReference: " + paymentReference)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getByAccount(String accountNumber) {
        return transactionRepository
                .findBySourceAccountNumberOrDestinationAccountNumber(accountNumber, accountNumber)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll() {
        return transactionRepository.findAll().stream().map(transactionMapper::toResponse).toList();
    }

    private String generateTransactionReference() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "TXN-" + LocalDate.now().format(DATE_FORMAT) + "-" + randomAlphaNumeric(6);
            if (!transactionRepository.existsByTransactionReference(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique transaction reference");
    }

    private String randomAlphaNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }


}
