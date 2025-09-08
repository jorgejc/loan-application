package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.request.LoanApplicationRequestDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/applications",
                    method = RequestMethod.POST,
                    beanClass = Handler.class,
                    beanMethod = "applyForLoanUseCase",
                    operation = @Operation(
                            operationId = "createLoanApplication",
                            summary = "apply for a loan",
                            description = "Submit a credit application and send us the application details",
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(schema = @Schema(implementation = LoanApplicationRequestDTO.class))
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Application successfully submitted",
                                            content = @Content(schema = @Schema(implementation = LoanApplicationResponseDTO.class))
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                                    @ApiResponse(responseCode = "404", description = "Loan type not found"),
                                    @ApiResponse(responseCode = "409", description = "Conflict (duplicate application)")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST("/api/v1/applications"), handler::applyForLoanUseCase);
    }
}
