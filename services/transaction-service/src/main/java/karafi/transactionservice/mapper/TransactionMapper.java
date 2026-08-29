package karafi.transactionservice.mapper;

import karafi.transactionservice.dto.TransactionResponse;
import karafi.transactionservice.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction t) {
       return TransactionResponse.builder()
               .id(t.getId())
               .transactionReference(t.getTransactionReference())
               .amount(t.getAmount())
               .currency(t.getCurrency())
               .status(t.getStatus())
               .transactionType(t.getTransactionType())
               .sourceAccountNumber(t.getSourceAccountNumber())
               .destinationAccountNumber(t.getDestinationAccountNumber())
               .completedAt(t.getCompletedAt())
               .createdAt(t.getCreatedAt())
               .build();
    }
}
