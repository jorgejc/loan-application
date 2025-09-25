package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.request.LoanApplicationRequestDTO;
import co.com.bancolombia.api.mapper.LoanApplicationMapper;
import co.com.bancolombia.usecase.loanapplication.LoanApplicationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * <b>Descripción:</b> Clase que determina
 * <br>
 * <b>HU02:</b> Registrar una solicitud de préstamo
 *
 * @author Jorge Armando Julio Cruz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Handler {

    private final LoanApplicationUseCase loanApplicationUseCase;
    private final LoanApplicationMapper loanApplicationMapper;
    private final RequestValidator requestValidator;

    /**
     * Método encargado de gestionar el envío de solicitudes de préstamo con autenticación y autorización JWT.
     * Los usuarios solo pueden enviar solicitudes para sí mismos, basándose en sus tokens.
     * <br>
     * <b>HU02:</b> Registrar una solicitud de prestamo
     *
     * @param serverRequest La solicitud entrante del servidor que contiene los datos de la solicitud de préstamo.
     * @return ServerResponse con la solicitud de préstamo creada o la respuesta de error.
     * @author Jorge Armando Julio Cruz
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")
    public Mono<ServerResponse> applyForLoanUseCase(ServerRequest serverRequest) {

        Mono<LoanApplicationRequestDTO> dtoMono = serverRequest.bodyToMono(LoanApplicationRequestDTO.class)
                .flatMap(requestValidator::validateUser);

        return Mono.zip(dtoMono, getCurrentJwtToken())
                .flatMap(tuple -> {
                    var dto = tuple.getT1();
                    var jwtAuth = tuple.getT2();

                    String tokenEmail = jwtAuth.getToken().getClaimAsString("email");
                    String tokenUserId = jwtAuth.getToken().getSubject();
                    String userRole = jwtAuth.getToken().getClaimAsString("role");

                    if (!isAuthorizedToCreateApplication(dto, tokenEmail, tokenUserId,userRole)) {
                        return Mono.error(new AccessDeniedException("You can only create loan applications for yourself"));
                    }

                    var applicationModel = loanApplicationMapper.toModel(dto);
                    log.info("Loan application submitted: {}", applicationModel.toString());

                    return loanApplicationUseCase.ApplyForLoan(applicationModel)
                            .doOnSuccess(saved -> log.info("Loan application saved: {}", saved.toString()));
                })
                .flatMap(savedApplication ->
                        ServerResponse.status(HttpStatus.CREATED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(loanApplicationMapper.toResponse(savedApplication))
                );
    }

    /**
     * Método encargado de extraer el token de autenticación JWT actual del contexto de seguridad reactivo.
     * <br>
     * <b>HU02:</b> Registrar una solicitud de prestamo
     *
     * @return Mono containing the JwtAuthenticationToken
     * @author Jorge Armando Julio Cruz
     */
    private Mono<JwtAuthenticationToken> getCurrentJwtToken() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (JwtAuthenticationToken) ctx.getAuthentication());
    }

    /**
     * Método encargado de verificar si el usuario actual está autorizado para crear una solicitud de préstamo.
     * Según la coincidencia del correo electrónico o el ID del cliente entre la solicitud y el token JWT.
     * <br>
     * <b>Caso de Uso:</b>
     *
     * @param dto el DTO de la solicitud de préstamo
     * @param tokenEmail Correo electrónico del token JWT
     * @param tokenUserId ID de usuario del token JWT (subreclamo)
     * @return true si está autorizado, false en caso contrario
     * @author Jorge Armando Julio Cruz
     */
    private boolean isAuthorizedToCreateApplication(LoanApplicationRequestDTO dto, String tokenEmail,
                                                    String tokenUserId, String userRole) {
        if (dto.email() != null && !dto.email().isBlank()) {
            return dto.email().equalsIgnoreCase(tokenEmail);
        }
        log.warn("Unable to validate user authorization - missing email or client ID in request");
        return false;
    }
}
