package account.service;

import account.dto.*;
import account.entity.Account;
import account.entity.AccountStatus;
import account.exception.*;
import account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;


@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImplt implements AccountService {

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



    public ApiResponse<AccountResponse> getAccount(String accountNumber) throws AccountNotFoundException {
        Account account = getAccountEntity(accountNumber);
        return new ApiResponse<>(HttpStatus.OK.value() , "Account found succesfuly " , toResponse(account));

    }



    public BalanceResponse getBalance(String accountNumber)  {

        Account account = getAccountEntity(accountNumber);

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
    public ApiResponse<AccountResponse> withdraw(String accountNumber, AmountRequest request) throws AccountNotFoundException {

        Account account = getAccountEntity(accountNumber);

        validateActiveAccount(account);

        validateAmount(request.amount());

        if (account.getBalance().compareTo(request.amount()) < 0) {

            throw new InsufficientBalanceException("Insufficient balance in this account");
        }

        account.setBalance(account.getBalance().subtract(request.amount()));

        return new ApiResponse<>(HttpStatus.OK.value() , "Withdrawal completed successfully" , toResponse(accountRepository.save(account)));

    }


    @Transactional
    public ApiResponse<AccountResponse> deposit(String accountNumber, AmountRequest request
    ) throws AccountNotFoundException {

        Account account = getAccountEntity(accountNumber);

        validateActiveAccount(account);

        validateAmount(request.amount());

        account.setBalance(account.getBalance().add(request.amount()));

        return new ApiResponse<>(HttpStatus.OK.value() , "Deposit completed successfully " , toResponse(accountRepository.save(account)));

    }



    @Transactional
    public ApiResponse<AccountResponse> blockAccount(String accountNumber) throws AccountNotFoundException {

        Account account = getAccountEntity(accountNumber);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed account cannot be blocked"
            );
        }

        account.setStatus(AccountStatus.BLOCKED);

        return new ApiResponse<>(HttpStatus.OK.value() , "Account blocked successfully " , toResponse(accountRepository.save(account)));

    }


    @Transactional
    public ApiResponse<AccountResponse> activateAccount(String accountNumber) throws AccountNotFoundException {

        Account account = getAccountEntity(accountNumber);

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

    private Account getAccountEntity(String accountNumber) throws AccountNotFoundException {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }
    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .fullName(account.getFullName())
                .accountNumber(account.getAccountNumber())
                .status(account.getStatus())
                .balance(account.getBalance())
                .CIN(account.getCin())
                .currency(account.getCurrency())
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
