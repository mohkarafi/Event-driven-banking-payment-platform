package account.controller;

import account.dto.*;
import account.service.AccountServiceImplt;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountServiceImplt accountService;


    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@RequestBody CreateAccountRequest request) {
       return ResponseEntity.ok(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNumber) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountNumber) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<ApiResponse<AccountResponse>> deposit(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.deposit(accountNumber, request));
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<ApiResponse<AccountResponse>> withdraw(@PathVariable String accountNumber, @Valid @RequestBody AmountRequest request) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.withdraw(accountNumber, request));
    }

    @PatchMapping("/{accountNumber}/block")
    public ResponseEntity<ApiResponse<AccountResponse>>  block(@PathVariable String accountNumber) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.blockAccount(accountNumber));
    }

    @PatchMapping("/{accountNumber}/activate")
    public ResponseEntity<ApiResponse<AccountResponse>>  activate(@PathVariable String accountNumber) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.activateAccount(accountNumber));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }
}
