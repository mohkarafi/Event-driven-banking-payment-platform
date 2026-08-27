package karafi.paymentservice.entity;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean canTransitionTo(PaymentStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING || target == CANCELLED;
            case PROCESSING -> target == COMPLETED || target == FAILED;
            case COMPLETED, FAILED, CANCELLED -> false;
        };
    }


}
