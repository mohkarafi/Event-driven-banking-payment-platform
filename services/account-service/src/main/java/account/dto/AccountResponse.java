package account.dto;

import account.entity.AccountStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountResponse(
        Long id ,
        String fullName,
        String email ,
        String accountNumber,
        BigDecimal balance,
        String currency,
        AccountStatus status
) {
}
