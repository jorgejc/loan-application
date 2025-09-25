package co.com.bancolombia.consumer;

import co.com.bancolombia.model.exceptions.ExternalServiceCommunicationException;
import co.com.bancolombia.model.loanapplication.gateways.IUserRestConsumer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * <b>Descripción:</b> Clase que determina el cliente REST reactivo para comunicación con servicios externos de usuarios,
 * implementando patrones de resiliencia como Circuit Breaker para manejo de fallos y degradación
 * controlada del servicio. Utiliza WebClient pre-configurado con propagación automática de tokens JWT.
 * <br>
 * <b>HU02:</b> Registrar una solicitud de préstamo
 *
 * @author Jorge Armando Julio Cruz
 */
@Slf4j
@Service
public class RestConsumer implements IUserRestConsumer {

    /**
     * Atributo que determina el nombre del servicio externo para logging y trazabilidad de errore
     */
    private static final String SERVICE_NAME = "user-service";

    /**
     * Atributo que determina el endpoint para validación de existencia de usuarios por email
     */
     private static final String PATH_VALIDATE_USER_BY_EMAIL = "/api/v1/users/email/{email}/exists";

    /**
     * Atributo que determina el cliente HTTP reactivo pre-configurado con autenticación JWT y URL base del servicio de usuarios
     */
     private final WebClient authServiceClient;

    public RestConsumer(@Qualifier("authServiceClient") WebClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    /**
     * Valida si un usuario existe en el sistema mediante su dirección de email.
     * Implementa Circuit Breaker para protección contra fallos del servicio externo.
     * @param email
     * @return
     */
    @Override
    @CircuitBreaker(name = "existsUserByEmail", fallbackMethod = "validateUserFallback")
    public Mono<Boolean> existsUserByEmail(String email) {
        log.debug("Validating user existence for email: {}", email);

        return authServiceClient.get()
                .uri(PATH_VALIDATE_USER_BY_EMAIL, email)
                .retrieve()
                .bodyToMono(ObjectResponse.class)
                .map(ObjectResponse::getExistsUser)
                .doOnSuccess(exists -> log.debug("User validation result for {}: {}", email, exists))
                .doOnError(error -> log.warn("Error validating user existence for email {}: {}", email, error.getMessage()));
    }

    /**
     * Método encargado de  fallback que se ejecuta automáticamente cuando el Circuit Breaker se activa debido a fallos
     * repetidos en la comunicación con el servicio externo de usuarios.
     * <br>
     * <b>HU03</b> Agregar autenticación al sistema
     *
     * @param email Email que se estaba intentando validar cuando ocurrió el fallo.
     * @param cause
     * @return cause Excepción original que causó la activación del fallback.
     * @throws ExternalServiceCommunicationException Excepción controlada que encapsula
     * el error original y proporciona contexto sobre el servicio que falló.
     * @see ExternalServiceCommunicationException
     * @see CircuitBreaker
     * @author Jorge Armando Julio Cruz
     */
    private Mono<Boolean> validateUserFallback(String email, Throwable cause) {
        log.error("Circuit breaker fallback activated for user validation - email: {}, cause: {}",
                email, cause.getMessage());

        return Mono.error(new ExternalServiceCommunicationException(
                SERVICE_NAME,
                PATH_VALIDATE_USER_BY_EMAIL,
                "External user service is temporarily unavailable. Unable to validate user existence at this time.",
                cause
        ));
    }
}
