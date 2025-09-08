package co.com.bancolombia.r2dbc.reactiverepositoryloantype;

import co.com.bancolombia.r2dbc.entities.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.entities.LoanTypeEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface LoanTypeReactiveRepository extends ReactiveCrudRepository
        <LoanTypeEntity, Integer>, ReactiveQueryByExampleExecutor<LoanTypeEntity> {

}
