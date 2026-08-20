package account.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AmountRequest(
        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Currency must contain 3 uppercase letters"
        )
        String currency,
        BigDecimal amount
) {
}
