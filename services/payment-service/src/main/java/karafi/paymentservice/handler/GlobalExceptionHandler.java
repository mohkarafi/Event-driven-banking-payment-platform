package karafi.paymentservice.handler;

import feign.FeignException;
import karafi.paymentservice.dto.ApiError;
import karafi.paymentservice.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(PaymentNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ApiError> handleInvalidPayment(InvalidPaymentException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT", ex.getMessage());
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ApiError> handleInvalidAmount(InvalidAmountException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", ex.getMessage());
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiError> handleAccountNotActive(AccountNotActiveException ex) {
        return build(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", ex.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(InsufficientBalanceException ex) {
        return build(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", ex.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyCompletedException.class)
    public ResponseEntity<ApiError> handleAlreadyCompleted(PaymentAlreadyCompletedException ex) {
        return build(HttpStatus.CONFLICT, "PAYMENT_ALREADY_COMPLETED", ex.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyCancelledException.class)
    public ResponseEntity<ApiError> handleAlreadyCancelled(PaymentAlreadyCancelledException ex) {
        return build(HttpStatus.CONFLICT, "PAYMENT_ALREADY_CANCELLED", ex.getMessage());
    }

    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<ApiError> handleInvalidState(InvalidPaymentStateException ex) {
        return build(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATE", ex.getMessage());
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ApiError> handleDuplicatePayment(DuplicatePaymentException ex) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", ex.getMessage());
    }

    @ExceptionHandler(PaymentAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleAlreadyExists(PaymentAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "PAYMENT_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ApiError> handleProcessingError(PaymentProcessingException ex) {
        log.error("Payment processing failure", ex);
        return build(HttpStatus.BAD_GATEWAY, "PAYMENT_PROCESSING_ERROR",
                "Payment could not be processed. Please try again later.");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException ex) {
        log.error("Downstream call to account-service failed: status={}", ex.status(), ex);
        return build(HttpStatus.BAD_GATEWAY, "ACCOUNT_SERVICE_UNAVAILABLE",
                "Could not reach account-service. Please try again later.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred.");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), error, message));
    }
}