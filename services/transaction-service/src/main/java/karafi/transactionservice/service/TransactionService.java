package karafi.transactionservice.service;

import karafi.transactionservice.dto.PaymentCompletedEvent;
import karafi.transactionservice.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {
    void createFromPaymentCompleted(PaymentCompletedEvent event);

    TransactionResponse getTransaction(Long id);

    TransactionResponse getByReference(String transactionReference);

    TransactionResponse getByPaymentReference(String paymentReference);

    List<TransactionResponse> getByAccount(String accountNumber);

    List<TransactionResponse> getAll();

}
