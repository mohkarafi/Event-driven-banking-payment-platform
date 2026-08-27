package karafi.transactionservice.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ApiError error,
        int status
) {
    public static <T> ApiResponse<T> ok(String message, T data, int status) {
        return new ApiResponse<>(true, message, data, null, status);
    }

    public static <T> ApiResponse<T> error(String message, ApiError error, int status) {
        return new ApiResponse<>(false, message, null, error, status);
    }
}