package co.com.bancolombia.api.exceptions;

import co.com.bancolombia.model.exceptions.ExternalServiceCommunicationException;
import io.r2dbc.spi.R2dbcException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setTitle("Forbidden");
        problemDetail.setDetail("You do not have permission to perform this action");

        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail));
    }

    @ExceptionHandler({InvalidBearerTokenException.class, AuthenticationException.class})
    public Mono<ResponseEntity<ProblemDetail>> handleAuthenticationErrors(Exception ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setTitle("Unauthorized");
        problemDetail.setDetail("Invalid or expired token");

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail));
    }

    @ExceptionHandler(ExternalServiceCommunicationException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleExternalServiceError(ExternalServiceCommunicationException ex) {
        log.error("External service communication failed: service={}, endpoint={}, error={}",
                ex.getService(), ex.getEndpoint(), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problemDetail.setTitle("External Service Unavailable");
        problemDetail.setDetail("The external service is temporarily unavailable. Please try again later.");

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail));
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleValidationError(ValidationException ex) {
        log.warn("Request validation failed: {}", ex.getMessage());

        ProblemDetail problemDetail = createValidationProblem(ex.getMessage());
        log.info("Responded with 400 - Validation Error");

        return Mono.just(ResponseEntity.badRequest().body(problemDetail));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument provided: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Request Data");
        problemDetail.setDetail(ex.getMessage());

        return Mono.just(ResponseEntity.badRequest().body(problemDetail));
    }

    @ExceptionHandler(DecodingException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleRequestDecodingError(DecodingException ex) {
        log.error("Request format error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Invalid Request Format");
        problemDetail.setDetail("The request body contains invalid JSON format or data types.");

        return Mono.just(ResponseEntity.badRequest().body(problemDetail));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleResponseStatus(ResponseStatusException ex) {
        if (ex.getStatusCode().value() == 404) {
            return handleNotFoundError(ex);
        }
        return handleGenericResponseStatus(ex);
    }

    @ExceptionHandler(R2dbcException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleDatabaseError(R2dbcException ex) {
        log.error("Database operation failed: {}", ex.getMessage());

        ProblemDetail problemDetail = createInternalServerError("A database error occurred while processing your request.");
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail));
    }

    @ExceptionHandler(ConnectException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleConnectionError(ConnectException ex) {
        log.error("Database connection failed: {}", ex.getMessage());

        ProblemDetail problemDetail = createInternalServerError("Unable to connect to the database. Please try again.");
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleUnexpectedError(Exception ex) {
        log.error("Unexpected system error: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = createInternalServerError("An unexpected error occurred. Please contact support if the problem persists.");
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail));
    }

    private ProblemDetail createValidationProblem(String errorMessage) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Request Validation Failed");

        List<String> errors = Arrays.asList(errorMessage.split("; "));
        problemDetail.setProperty("errors", errors);
        problemDetail.setDetail("Please check the following validation errors and correct your request.");

        return problemDetail;
    }

    private Mono<ResponseEntity<ProblemDetail>> handleNotFoundError(ResponseStatusException ex) {
        log.error("Resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setDetail("The requested resource could not be found.");

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail));
    }

    private Mono<ResponseEntity<ProblemDetail>> handleGenericResponseStatus(ResponseStatusException ex) {
        log.error("Response status error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatusCode());
        problemDetail.setTitle("Request Processing Error");
        problemDetail.setDetail("An error occurred while processing your request.");

        return Mono.just(ResponseEntity.status(ex.getStatusCode()).body(problemDetail));
    }

    private ProblemDetail createInternalServerError(String message) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setDetail(message);
        return problemDetail;
    }

}
