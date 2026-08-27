package karafi.paymentservice.mapper;

import karafi.paymentservice.dto.PaymentResponse;
import karafi.paymentservice.entity.Payment;
import org.hibernate.validator.constraints.CodePointLength;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getSourceAccountNumber(),
                payment.getDestinationAccountNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getDescription(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
