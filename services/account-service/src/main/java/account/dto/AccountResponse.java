package account.dto;

import account.entity.AccountStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountResponse(
        String fullName,
        String accountNumber,
        AccountStatus status
) {
}
