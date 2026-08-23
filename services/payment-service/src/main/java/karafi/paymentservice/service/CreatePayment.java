package karafi.paymentservice.service;

import karafi.paymentservice.dto.ApiResponse;
import karafi.paymentservice.dto.CreatePaymentRequest;
import karafi.paymentservice.dto.PaymentResponse;
import karafi.paymentservice.entity.Payment;
import karafi.paymentservice.exception.InvalidAmountException;
import karafi.paymentservice.exception.InvalidPaymentException;
import karafi.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreatePayment {
     private final PaymentRepository paymentRepository;

    public ApiResponse<PaymentResponse> createPayment(CreatePaymentRequest request , String idemKey){

        if(idemKey == null || idemKey.isBlank()){
            throw  new InvalidPaymentException("idemKEy must not be null or blank");
        }
        var existing = paymentRepository.findByIdempotencyKey(idemKey);

        if(existing.isPresent()){
            return new ApiResponse(201 , "Payment Already Exist" , existing);
        }
        if(request.sourceAccountNumber().equals(request.destinationAccountNumber())){
            throw new InvalidPaymentException("Source number and destination number must be different");
        }
        if(request.amount().compareTo(BigDecimal.ZERO) <= 0){
            throw new InvalidAmountException("Amount must be greater than 0");
        }
        Payment payment = Payment.builder()
                .paymentReference("PAY-"+ LocalDateTime.now().toString())
                .idempotencyKey(idemKey)
                .sourceAccountNumber(request.sourceAccountNumber())
                .destinationAccountNumber(request.destinationAccountNumber())
                .amount(request.amount())
                .currency(request.currency())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);
        return new ApiResponse(201 , "Payment Created" , payment);
    }
}
