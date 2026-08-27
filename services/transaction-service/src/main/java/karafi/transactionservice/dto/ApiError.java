package karafi.transactionservice.dto;

import java.time.LocalDateTime;

public record ApiError(
        String code,
        String message,
        String path,
        LocalDateTime timestamp
) {
    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, path, LocalDateTime.now());
    }
}