package karafi.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String COMPLETED_TOPIC = "payment.completed";
    private static final String FAILED_TOPIC = "payment.failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;


    private final ExecutorService kafkaExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kafka-publisher");
        t.setDaemon(true);
        return t;
    });

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        publishAsync(COMPLETED_TOPIC, event.paymentReference(), event, "PaymentCompletedEvent");
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        publishAsync(FAILED_TOPIC, event.paymentReference(), event, "PaymentFailedEvent");
    }

    private void publishAsync(String topic, String key, Object event, String eventName) {
        kafkaExecutor.submit(() -> {
            try {
                kafkaTemplate.send(topic, key, event).get(5, TimeUnit.SECONDS);
                log.info("Published {}: reference={}", eventName, key);
            } catch (Exception e) {
                log.error("Failed to publish {} for reference={}. Business operation already "
                                + "completed; this event publication will not be retried automatically.",
                        eventName, key, e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        kafkaExecutor.shutdown();
    }
}