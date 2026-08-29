package karafi.transactionservice.dto;

import karafi.transactionservice.entity.TransactionStatus;
import karafi.transactionservice.entity.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
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