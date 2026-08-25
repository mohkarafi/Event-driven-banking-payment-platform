package karafi.paymentservice.kafka;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentFailedEvent(
        String paymentReference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String reason,
        LocalDateTime failedAt
) {
}
