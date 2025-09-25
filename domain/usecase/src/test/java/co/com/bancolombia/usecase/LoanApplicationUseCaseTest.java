/**
 * LoanApplicationUseCaseTest.java
 */
package co.com.bancolombia.usecase;

import co.com.bancolombia.model.exceptions.LoanTypeNotFoundException;
import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.IUserRestConsumer;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loantype.LoanType;
import co.com.bancolombia.model.loantype.gateways.LoanTypeRepository;
import co.com.bancolombia.usecase.loanapplication.LoanApplicationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

public class LoanApplicationUseCaseTest {

    @Mock
    private LoanTypeRepository loanTypeRepository;
    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private IUserRestConsumer userRestConsumer;

    private LoanApplicationUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new LoanApplicationUseCase(loanTypeRepository, loanApplicationRepository, userRestConsumer);
    }

    @Test
    void applyForLoan_WhenUserExistsAndLoanTypeIsValid_ShouldSaveLoanApplication() {
        // Given
        LoanApplication application = LoanApplication.builder()
                .email("test@example.com")
                .amount(BigDecimal.valueOf(5000.26))
                .term(12)
                .loanTypeId(1)
                .build();

        when(userRestConsumer.existsUserByEmail("test@example.com")).thenReturn(Mono.just(true));
        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(mock(LoanType.class)));
        when(loanApplicationRepository.save(application)).thenReturn(Mono.just(application));

        // When
        Mono<LoanApplication> result = useCase.ApplyForLoan(application);

        // Then
        StepVerifier.create(result)
                .expectNext(application)
                .verifyComplete();

        verify(userRestConsumer).existsUserByEmail("test@example.com");
        verify(loanTypeRepository).findById(1);
        verify(loanApplicationRepository).save(application);
    }

    @Test
    void applyForLoan_WhenUserDoesNotExist_ShouldReturnError() {
        // Given
        LoanApplication application = LoanApplication.builder()
                .email("notfound@example.com")
                .loanTypeId(2)
                .build();

        when(userRestConsumer.existsUserByEmail("notfound@example.com")).thenReturn(Mono.just(false));

        // When
        Mono<LoanApplication> result = useCase.ApplyForLoan(application);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().contains("User doesn't exists"))
                .verify();

        verify(userRestConsumer).existsUserByEmail("notfound@example.com");
        verifyNoInteractions(loanTypeRepository, loanApplicationRepository);
    }

    @Test
    void applyForLoan_WhenLoanTypeDoesNotExist_ShouldReturnError() {
        // Given
        LoanApplication application = LoanApplication.builder()
                .email("user@example.com")
                .loanTypeId(99)
                .build();

        when(userRestConsumer.existsUserByEmail("user@example.com")).thenReturn(Mono.just(true));
        when(loanTypeRepository.findById(99)).thenReturn(Mono.empty());

        // When
        Mono<LoanApplication> result = useCase.ApplyForLoan(application);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof LoanTypeNotFoundException)
                .verify();

        verify(userRestConsumer).existsUserByEmail("user@example.com");
        verify(loanTypeRepository).findById(99);
        verifyNoInteractions(loanApplicationRepository);
    }

}
