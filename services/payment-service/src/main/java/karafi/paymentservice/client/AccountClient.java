package karafi.paymentservice.client;

import karafi.paymentservice.dto.AccountDto;
import karafi.paymentservice.dto.AmountRequest;
import karafi.paymentservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@FeignClient(name = "account-service")
@Component
public interface AccountClient {
    @GetMapping("/api/accounts/{accountNumber}")
    ApiResponse<AccountDto> getAccountByNumber(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/accounts/{accountNumber}/withdraw")
    ApiResponse<AccountDto> withdraw(@PathVariable("accountNumber") String accountNumber, @RequestBody AmountRequest request);

    @PostMapping("/api/accounts/{accountNumber}/deposit")
    ApiResponse<AccountDto> deposit(@PathVariable("accountNumber") String accountNumber, @RequestBody AmountRequest request);
}
