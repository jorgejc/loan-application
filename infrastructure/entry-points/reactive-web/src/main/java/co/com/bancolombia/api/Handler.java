package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.request.LoanApplicationRequestDTO;
import co.com.bancolombia.api.mapper.LoanApplicationMapper;
import co.com.bancolombia.usecase.loanapplication.LoanApplicationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class Handler {

    private final LoanApplicationUseCase loanApplicationUseCase;
    private final LoanApplicationMapper loanApplicationMapper;
    private final RequestValidator requestValidator;

    public Mono<ServerResponse> applyForLoanUseCase(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(LoanApplicationRequestDTO.class)
                .flatMap(requestValidator::validateUser)
                .map(loanApplicationMapper::toModel)
                .flatMap(applicationReq -> {
                    log.info("Application subitted: {}", applicationReq.toString());
                    return loanApplicationUseCase.ApplyForLoan(applicationReq)
                            .doOnSuccess(saved -> log.info("Application save: {}", saved.toString()));
                })
                .flatMap(savedApplication -> ServerResponse.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loanApplicationMapper.toResponse(savedApplication)));
    }
}
