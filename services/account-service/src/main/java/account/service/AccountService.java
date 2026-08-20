package account.service;

import account.dto.AccountResponse;
import account.dto.AmountRequest;
import account.dto.BalanceResponse;
import account.dto.CreateAccountRequest;
import account.entity.Account;
import account.entity.AccountStatus;
import account.exception.AccountBlockedException;
import account.exception.InsufficientBalanceException;
import account.exception.InvalidAmountException;
import account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .fullName(request.fullName())
                .email(request.email())
                .accountNumber(accountNumber)
                .currency(request.currency())
                .balance(BigDecimal.ZERO)
                .build();


        Account savedAccount = accountRepository.save(account);

        return toResponse(savedAccount);
    }

    public AccountResponse getAccount(Long accountId) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        return toResponse(account);
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
    public AccountResponse withdraw(Long accountId, AmountRequest request) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        validateActiveAccount(account);

        validateAmount(request.amount());

        if (account.getBalance().compareTo(request.amount()) < 0) {

            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.amount()));

        return toResponse(accountRepository.save(account));
    }


    @Transactional
    public AccountResponse deposit(Long accountId, AmountRequest request
    ) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        validateActiveAccount(account);

        validateAmount(request.amount());

        account.setBalance(account.getBalance().add(request.amount())
        );

        return toResponse(accountRepository.save(account));
    }



    @Transactional
    public AccountResponse blockAccount(Long accountId) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed account cannot be blocked"
            );
        }

        account.setStatus(AccountStatus.BLOCKED);

        return toResponse(
                accountRepository.save(account)
        );
    }


    @Transactional
    public AccountResponse activateAccount(Long accountId) throws AccountNotFoundException {

        Account account = getAccountEntity(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException(
                    "Closed account cannot be activated"
            );
        }

        account.setStatus(AccountStatus.ACTIVE);

        return toResponse(accountRepository.save(account)
        );
    }






    private void validateActiveAccount(Account account) {

        if (account.getStatus() != AccountStatus.ACTIVE) {

            throw new AccountBlockedException(
                    "Account is not active"
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
                .id(account.getId())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }

    private String generateAccountNumber() {
        return "MA-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();
    }


}
