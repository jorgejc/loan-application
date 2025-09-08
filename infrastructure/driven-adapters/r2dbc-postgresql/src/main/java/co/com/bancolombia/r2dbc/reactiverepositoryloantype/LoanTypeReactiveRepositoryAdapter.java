package co.com.bancolombia.r2dbc.reactiverepositoryloantype;

import co.com.bancolombia.model.loantype.LoanType;
import co.com.bancolombia.model.loantype.gateways.LoanTypeRepository;
import co.com.bancolombia.r2dbc.entities.LoanTypeEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class LoanTypeReactiveRepositoryAdapter extends ReactiveAdapterOperations
        <LoanType, LoanTypeEntity, Integer, LoanTypeReactiveRepository> implements LoanTypeRepository {

    public LoanTypeReactiveRepositoryAdapter(LoanTypeReactiveRepository repository, ObjectMapper mapper) {

        super(repository, mapper, LoanTypeEntity -> mapper.map(LoanTypeEntity, LoanType.class));
    }

    @Override
    public Mono<LoanType> findById(Integer loanTypeId) {
        return repository.findById(loanTypeId).map(this::toEntity);
    }
}
