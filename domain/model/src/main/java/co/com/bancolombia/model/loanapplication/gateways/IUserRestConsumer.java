package co.com.bancolombia.model.loanapplication.gateways;

import reactor.core.publisher.Mono;

public interface IUserRestConsumer {

    Mono<Boolean> existsUserByEmail(String email);
}
