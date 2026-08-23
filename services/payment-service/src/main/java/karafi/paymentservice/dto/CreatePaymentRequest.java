package karafi.paymentservice.dto;

import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String description
) {
}