package co.com.bancolombia.model.loanapplication;
import lombok.*;

import java.math.BigDecimal;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanApplication {

    private String loanApplicationId;
    private BigDecimal amount;
    private Integer term;
    private String email;
    private String documentId;
    private Integer statusId;
    private Integer loanTypeId;

}
