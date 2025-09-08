package co.com.bancolombia.r2dbc.reactiverepositoryloanapplication;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.r2dbc.entities.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LoanApplicationReactiveRepositoryAdapter extends ReactiveAdapterOperations
        <LoanApplication, LoanApplicationEntity, String, LoanApplicationReactiveRepository> implements LoanApplicationRepository {

    public LoanApplicationReactiveRepositoryAdapter(LoanApplicationReactiveRepository repository, ObjectMapper mapper) {

        super(repository, mapper, LoanApplicationEntity -> mapper.map(LoanApplicationEntity, LoanApplication.class));
    }

}
