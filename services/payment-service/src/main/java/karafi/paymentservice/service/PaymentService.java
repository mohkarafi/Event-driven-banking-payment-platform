package karafi.paymentservice.service;

import karafi.paymentservice.dto.ApiResponse;
import karafi.paymentservice.dto.CreatePaymentRequest;
import karafi.paymentservice.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {
    ApiResponse<PaymentResponse> createPayment(CreatePaymentRequest request, String idempotencyKey);
    ApiResponse<PaymentResponse> getPayment(Long paymentId);
    ApiResponse<PaymentResponse> getPaymentByReference(String paymentReference);
    ApiResponse<List<PaymentResponse>> getPaymentsByAccount(String accountNumber);
    ApiResponse<PaymentResponse> executePayment(Long paymentId);
    ApiResponse<PaymentResponse> cancelPayment(Long paymentId);
}
