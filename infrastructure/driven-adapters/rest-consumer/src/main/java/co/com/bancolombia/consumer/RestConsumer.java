package co.com.bancolombia.consumer;

import co.com.bancolombia.model.exceptions.ExternalServiceCommunicationException;
import co.com.bancolombia.model.loanapplication.gateways.IUserRestConsumer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestConsumer implements IUserRestConsumer {

    private final WebClient client;
    private static final String SERVICE_NAME = "user-service";
    private static final String PATH_VALIDATE_USER_BY_EMAIL = "/api/v1/users/email/{email}/exists";


    @Override
    @CircuitBreaker(name = "existsUserByEmail", fallbackMethod = "validateUserFallback")
    public Mono<Boolean> existsUserByEmail(String email) {
        return client.get().uri(PATH_VALIDATE_USER_BY_EMAIL,  email)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(new ExternalServiceCommunicationException(
                                SERVICE_NAME, PATH_VALIDATE_USER_BY_EMAIL,
                                "Error 4xx al validar usuario por email: " + email +
                                        " (status=" + resp.statusCode().value() + ", body=" + body + ")", null)))
                )
                .onStatus(HttpStatusCode::is5xxServerError, resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new ExternalServiceCommunicationException(
                                        SERVICE_NAME, PATH_VALIDATE_USER_BY_EMAIL,
                                        "Error 5xx comunicando con " + SERVICE_NAME +
                                                " al validar email: " + email +
                                                " (status=" + resp.statusCode().value() + ", body=" + body + ")",
                                        null)))
                )
                .bodyToMono(ObjectResponse.class)
                .map(ObjectResponse::getExistsUser);
    }
    
    private Mono<Boolean> validateUserFallback(String email, Throwable cause) {
        log.error("Fallback activado para validar email {}. Causa: {}", email, cause.toString());
        return Mono.error(new ExternalServiceCommunicationException(
                SERVICE_NAME,
                PATH_VALIDATE_USER_BY_EMAIL,
                "Communication problems with the external service, it is not possible to process the user validation request.",
                cause
        ));
    }

}
