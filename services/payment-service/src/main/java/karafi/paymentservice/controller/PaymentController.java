package karafi.paymentservice.controller;

import jakarta.validation.Valid;
import karafi.paymentservice.dto.ApiResponse;
import karafi.paymentservice.dto.CreatePaymentRequest;
import karafi.paymentservice.dto.PaymentResponse;
import karafi.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody CreatePaymentRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey) {
        ApiResponse<PaymentResponse> response = paymentService.createPayment(request, idempotencyKey);
        return ResponseEntity.status(response.status()).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @GetMapping("/reference/{paymentReference}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByReference(@PathVariable String paymentReference) {
        return ResponseEntity.ok(paymentService.getPaymentByReference(paymentReference));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(paymentService.getPaymentsByAccount(accountNumber));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<PaymentResponse>> executePayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.executePayment(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.cancelPayment(id));
    }
}
