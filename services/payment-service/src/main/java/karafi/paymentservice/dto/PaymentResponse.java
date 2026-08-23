package karafi.paymentservice.dto;

import karafi.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentReference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String description,
        PaymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}