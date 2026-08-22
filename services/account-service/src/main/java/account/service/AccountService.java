package account.service;

import account.dto.*;
import account.entity.Account;
import account.entity.AccountStatus;
import account.exception.AccountAlreadyExistsException;
import account.exception.AccountBlockedException;
import account.exception.InsufficientBalanceException;
import account.exception.InvalidAmountException;
import account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.security.SecureRandom;


@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {
    private final AccountRepository accountRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ApiResponse<AccountResponse> createAccount(CreateAccountRequest request) {

        if(accountRepository.existsByCin(request.CIN())){
            throw new AccountAlreadyExistsException("An Account with this CIN already exists");
        }
        String accountNumber = generateAccountNumber();
        Account account = Account.builder()
                .fullName(request.fullName())
                .email(request.email())
                .accountNumber(accountNumber)
                .currency(request.currency())
                .balance(BigDecimal.ZERO)
                .cin(request.CIN())
                .status(AccountStatus.ACTIVE)
                .build();


        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        return new ApiResponse<>(HttpStatus.CREATED.value() , "Account created succesfuly " ,  toResponse(savedAccount));
    }



    public ApiResponse<AccountResponse> getAccount(Long accountId) throws AccountNotFoundException {
        Account account = getAccountEntity(accountId);
        return new ApiResponse<>(HttpStatus.OK.value() , "Account found succesfuly " , toResponse(account));

    }



    public BalanceResponse getBalance(Long accountId) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        BalanceResponse balanceResponse = BalanceResponse.builder()
                .balance(account.getBalance())
                .accountNumber(account.getAccountNumber())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
        return balanceResponse;
    }



    @Transactional
    public ApiResponse<AccountResponse> withdraw(Long accountId, AmountRequest request) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        validateActiveAccount(account);

        validateAmount(request.amount());

        if (account.getBalance().compareTo(request.amount()) < 0) {

            throw new InsufficientBalanceException("Insufficient balance in this account");
        }

        account.setBalance(account.getBalance().subtract(request.amount()));

        return new ApiResponse<>(HttpStatus.OK.value() , "Withdrawal completed successfully" , toResponse(accountRepository.save(account)));

    }


    @Transactional
    public ApiResponse<AccountResponse> deposit(Long accountId, AmountRequest request
    ) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        validateActiveAccount(account);

        validateAmount(request.amount());

        account.setBalance(account.getBalance().add(request.amount()));

        return new ApiResponse<>(HttpStatus.OK.value() , "Deposit completed successfully " , toResponse(accountRepository.save(account)));

    }



    @Transactional
    public ApiResponse<AccountResponse> blockAccount(Long accountId) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed account cannot be blocked"
            );
        }

        account.setStatus(AccountStatus.BLOCKED);

        return new ApiResponse<>(HttpStatus.OK.value() , "Account blocked successfully " , toResponse(accountRepository.save(account)));

    }


    @Transactional
    public ApiResponse<AccountResponse> activateAccount(Long accountId) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed account cannot be activated"
            );
        }

        account.setStatus(AccountStatus.ACTIVE);


        return new ApiResponse<>(HttpStatus.OK.value() , "Account activated successfully. " , toResponse(accountRepository.save(account)));

    }






    private void validateActiveAccount(Account account) {

        if (account.getStatus() != AccountStatus.ACTIVE) {

            throw new AccountBlockedException(
                    "this account is not active"
            );
        }
    }

    private void validateAmount(BigDecimal amount) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new InvalidAmountException(
                    "Amount must be greater than zero"
            );
        }
    }

    private Account getAccountEntity(Long accountId) throws AccountNotFoundException {

        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(
                                "Account not found: " + accountId
                        )
                );
    }
    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .fullName(account.getFullName())
                .accountNumber(account.getAccountNumber())
                .status(account.getStatus())
                .build();
    }

    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder(24);
        // Premier chiffre non nul pour garantir exactement 24 chiffres (pas de zéro en tête)
        sb.append(1 + RANDOM.nextInt(9));
        for (int i = 1; i < 24; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }


}
