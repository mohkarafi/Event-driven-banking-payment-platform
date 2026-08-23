package karafi.paymentservice.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        String paymentReference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        LocalDateTime completedAt
) {
}
