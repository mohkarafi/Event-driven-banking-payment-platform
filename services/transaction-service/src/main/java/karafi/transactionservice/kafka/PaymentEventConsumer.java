package karafi.transactionservice.kafka;

import karafi.transactionservice.dto.PaymentCompletedEvent;
import karafi.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final TransactionService transactionService;

    @KafkaListener(topics = "payment.completed", groupId = "transaction-service")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent: paymentReference={}", event.paymentReference());
        transactionService.createFromPaymentCompleted(event);
    }

}
