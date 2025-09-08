package co.com.bancolombia.r2dbc.reactiverepositoryloanapplication;

import co.com.bancolombia.r2dbc.entities.LoanApplicationEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface LoanApplicationReactiveRepository extends ReactiveCrudRepository
        <LoanApplicationEntity, String>, ReactiveQueryByExampleExecutor<LoanApplicationEntity> {

}
