package account.controller;

import account.dto.*;
import account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;


    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@RequestBody CreateAccountRequest request) {
       return ResponseEntity.ok(accountService.createAccount(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long id) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long id) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<AccountResponse>> deposit(@PathVariable Long id, @Valid @RequestBody AmountRequest request) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.deposit(id, request));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<AccountResponse>> withdraw(@PathVariable Long id, @Valid @RequestBody AmountRequest request) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.withdraw(id, request));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<ApiResponse<AccountResponse>>  block(@PathVariable Long id) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.blockAccount(id));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AccountResponse>>  activate(@PathVariable Long id) throws AccountNotFoundException {
        return ResponseEntity.ok(accountService.activateAccount(id));
    }
}
