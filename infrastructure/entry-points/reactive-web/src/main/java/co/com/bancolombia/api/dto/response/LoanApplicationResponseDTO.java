package co.com.bancolombia.api.dto.response;

import java.math.BigDecimal;

public record LoanApplicationResponseDTO(
        String id,
        BigDecimal amount,
        Integer term,
        String email,
        String documentId,
        Integer loanTypeId,
        Integer statusId
) {
}
