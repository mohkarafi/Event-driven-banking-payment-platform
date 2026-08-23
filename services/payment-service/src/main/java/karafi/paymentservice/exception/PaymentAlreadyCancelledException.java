package karafi.paymentservice.exception;

public class PaymentAlreadyCancelledException extends RuntimeException {
    public PaymentAlreadyCancelledException(String message) {
        super(message);
    }
}
