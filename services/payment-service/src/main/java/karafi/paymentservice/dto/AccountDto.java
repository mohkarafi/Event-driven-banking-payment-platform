package karafi.paymentservice.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder

public record AccountDto(
        String accountNumber,
        BigDecimal balance,
        String currency,
        AccountStatus status

) {
}
