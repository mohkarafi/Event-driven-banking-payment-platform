package account.service;

import account.dto.*;
import account.exception.AccountNotFoundException;

public interface AccountService {

    ApiResponse<AccountResponse> createAccount(CreateAccountRequest request) ;

    ApiResponse<AccountResponse> getAccount(String accountNumber) throws AccountNotFoundException;

    BalanceResponse getBalance(String accountNumber);

    ApiResponse<AccountResponse> withdraw(String accountNumber, AmountRequest request) ;

    ApiResponse<AccountResponse> deposit(String accountNumber, AmountRequest request);

    ApiResponse<AccountResponse> blockAccount(String accountNumber) throws AccountNotFoundException;

    ApiResponse<AccountResponse> activateAccount(String accountNumber) throws AccountNotFoundException;
}