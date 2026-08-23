package karafi.paymentservice.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private static final String COMPLETED_TOPIC = "payment.completed";
    private static final String FAILED_TOPIC = "payment.failed";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        kafkaTemplate.send(COMPLETED_TOPIC, event.paymentReference(), event);
        log.info("Published PaymentCompletedEvent: reference={}", event.paymentReference());
    }
    public void publishPaymentFailed(PaymentFailedEvent event) {
        kafkaTemplate.send(FAILED_TOPIC, event.paymentReference(), event);
        log.info("Published PaymentFailedEvent: reference={}, reason={}",
                event.paymentReference(), event.reason());
    }
}
