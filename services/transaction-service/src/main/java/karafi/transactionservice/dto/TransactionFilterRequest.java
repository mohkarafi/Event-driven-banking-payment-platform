package karafi.transactionservice.dto;

import java.time.LocalDateTime;

public record TransactionFilterRequest(
        String accountNumber,
        TransactionStatus status,
        LocalDateTime from,
        LocalDateTime to
) {
}