package karafi.paymentservice.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AmountRequest(
        String currency,
        BigDecimal amount
) {
}
