package karafi.transactionservice.repository;

import karafi.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionReference(String transactionReference);

    Optional<Transaction> findByPaymentReference(String paymentReference);

    boolean existsByPaymentReference(String paymentReference);

    boolean existsByTransactionReference(String transactionReference);

    List<Transaction> findBySourceAccountNumber(String accountNumber);

    List<Transaction> findByDestinationAccountNumber(String accountNumber);

    List<Transaction> findBySourceAccountNumberOrDestinationAccountNumber(String sourceAccountNumber, String destinationAccountNumber);
}
