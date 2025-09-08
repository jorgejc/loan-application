package co.com.bancolombia.model.loantype.gateways;

import co.com.bancolombia.model.loantype.LoanType;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface LoanTypeRepository {

    Mono<LoanType> findById(Integer loanTypeId);

    /**
     *  get details of the loan type
     * Mono<Boolean> existsById(Long loanTypeId);
     *
     *  To validate that the amount is within the allowed range
     * Mono<Boolean> isAmountInRange(Long loanTypeId, BigDecimal amount);
     *
     **/
}
