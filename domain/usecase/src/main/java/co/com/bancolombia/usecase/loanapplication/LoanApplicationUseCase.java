package co.com.bancolombia.usecase.loanapplication;

import co.com.bancolombia.model.exceptions.LoanTypeNotFoundException;
import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.IUserRestConsumer;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loantype.gateways.LoanTypeRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class LoanApplicationUseCase {

    private final LoanTypeRepository loanTypeRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final IUserRestConsumer userRestConsumer;

    public Mono<LoanApplication> ApplyForLoan(LoanApplication loanApplication){
        return userRestConsumer.existsUserByEmail(loanApplication.getEmail())
                .flatMap(existUser -> Boolean.TRUE.equals(existUser)
                        ? internalManagement(loanApplication)
                        : Mono.error(new IllegalArgumentException("User doesn't exists")));
    }

        private Mono<LoanApplication> internalManagement(LoanApplication loanApplication){
            return Mono.just(loanApplication)
                    .flatMap(loan -> validateLoanType(loanApplication.getLoanTypeId())
                            .thenReturn(loan))
                    .flatMap(loanApplicationRepository::save);
        }

        private Mono<Void> validateLoanType(Integer loanTypeId){
            return loanTypeRepository.findById(loanTypeId)
                    .switchIfEmpty(Mono.error(new LoanTypeNotFoundException(loanTypeId)))
                    .then();
        }
}
