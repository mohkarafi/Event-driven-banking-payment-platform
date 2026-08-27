package karafi.paymentservice.handler;


import karafi.paymentservice.dto.ApiError;
import karafi.paymentservice.exception.AccountNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class Global {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> HandleAccountNotException(AccountNotFoundException ex){
        return build(HttpStatus.NOT_FOUND , "ACCOUNT_NOT_FOUND" , ex.getMessage());
    }

    public ResponseEntity<ApiError> build(HttpStatus status , String error , String message){
        return ResponseEntity.status(status).body(ApiError.of(status.value() , error , message));
    }
}
