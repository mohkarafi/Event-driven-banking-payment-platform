package account.exception;

import account.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(
            AccountNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        404,
                        "ACCOUNT_NOT_FOUND",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleAccountAlreadyExists(
            AccountAlreadyExistsException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        409,
                        "ACCOUNT_ALREADY_EXISTS",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ApiError> handleAccountBlocked(
            AccountBlockedException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        409,
                        "ACCOUNT_BLOCKED",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ApiError> handleInvalidAmount(
            InvalidAmountException ex) {

        return ResponseEntity
                .badRequest()
                .body(new ApiError(
                        400,
                        "INVALID_AMOUNT",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(
            InsufficientBalanceException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        409,
                        "INSUFFICIENT_BALANCE",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidAccountStateException.class)
    public ResponseEntity<ApiError> handleInvalidAccountState(
            InvalidAccountStateException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        409,
                        "INVALID_ACCOUNT_STATE",
                        ex.getMessage()
                ));
    }
}