package co.com.bancolombia.r2dbc.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


import java.math.BigDecimal;

@Table("applications")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class LoanApplicationEntity {

    @Id
    @Column("application_id")
    private String loanApplicationId;

    private BigDecimal amount;

    private Integer term;

    private String email;

    @Column("document_id")
    private String documentId;

    @Column("loan_type_id")
    private Integer loanTypeId;

    @Column("status_id")
    private Integer statusId;
}
