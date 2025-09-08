package co.com.bancolombia.api.mapper;

import co.com.bancolombia.api.dto.request.LoanApplicationRequestDTO;
import co.com.bancolombia.api.dto.response.LoanApplicationResponseDTO;
import co.com.bancolombia.model.loanapplication.LoanApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoanApplicationMapper {

    @Mapping(source = "loanApplicationId", target = "id")
    LoanApplicationResponseDTO toResponse(LoanApplication loanApplication);

    @Mapping(target = "loanApplicationId", ignore = true)
    @Mapping(target = "statusId", constant = "1")
    LoanApplication toModel(LoanApplicationRequestDTO loanApplicationRequestDTO);
}
