package karafi.transactionservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String transactionReference,
        String paymentReference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        TransactionType transactionType,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}