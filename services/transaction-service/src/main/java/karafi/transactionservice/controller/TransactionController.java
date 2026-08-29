package karafi.transactionservice.controller;

import karafi.transactionservice.dto.ApiResponse;
import karafi.transactionservice.dto.TransactionResponse;
import karafi.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable Long id) {
        TransactionResponse response = transactionService.getTransaction(id);
        return ResponseEntity.ok(ApiResponse.ok("Transaction retrieved successfully.", response, 200));
    }

    @GetMapping("/reference/{transactionReference}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(
            @PathVariable String transactionReference) {
        TransactionResponse response = transactionService.getByReference(transactionReference);
        return ResponseEntity.ok(ApiResponse.ok("Transaction retrieved successfully.", response, 200));
    }

    @GetMapping("/payment/{paymentReference}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByPaymentReference(
            @PathVariable String paymentReference) {
        TransactionResponse response = transactionService.getByPaymentReference(paymentReference);
        return ResponseEntity.ok(ApiResponse.ok("Transaction retrieved successfully.", response, 200));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getByAccount(
            @PathVariable String accountNumber) {
        List<TransactionResponse> response = transactionService.getByAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved successfully.", response, 200));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAll() {
        List<TransactionResponse> response = transactionService.getAll();
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved successfully.", response, 200));
    }
}
