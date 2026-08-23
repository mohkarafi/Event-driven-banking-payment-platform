package karafi.paymentservice.repository;

import karafi.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByPaymentReference(String paymentReference);
    boolean existsByPaymentReference(String paymentReference);
    List<Payment> findBySourceAccountNumberOrDestinationAccountNumber(String sourceAccountNumber, String destinationAccountNumber);
}
