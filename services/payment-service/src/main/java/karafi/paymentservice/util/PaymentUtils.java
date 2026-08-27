package karafi.paymentservice.util;

import karafi.paymentservice.exception.PaymentProcessingException;
import karafi.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class PaymentUtils {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final PaymentRepository paymentRepository;

    public String randomAlphaNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    public String generatePaymentReference() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "PAY-" + LocalDate.now().format(DATE_FORMAT) + "-" + randomAlphaNumeric(4);
            if (!paymentRepository.existsByPaymentReference(candidate)) {
                return candidate;
            }
        }
        throw new PaymentProcessingException("Unable to generate a unique payment reference");
    }
}
