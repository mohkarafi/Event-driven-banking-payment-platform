package karafi.paymentservice.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
