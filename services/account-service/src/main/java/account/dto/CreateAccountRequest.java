package account.dto;

import lombok.Builder;

@Builder
public record CreateAccountRequest(
        String fullName,
        String email,
        String CIN,
        String currency
) {
}
