package karafi.transactionservice.handler;



import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import karafi.transactionservice.dto.ApiError;
import karafi.transactionservice.dto.ApiResponse;
import karafi.transactionservice.exception.DuplicateTransactionException;
import karafi.transactionservice.exception.InvalidTransactionException;
import karafi.transactionservice.exception.TransactionNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(TransactionNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateTransactionException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(InvalidTransactionException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "This transaction already exists.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.", req);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message, HttpServletRequest req) {
        ApiError error = ApiError.of(code, message, req.getRequestURI());
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, error, status.value()));
    }

}
