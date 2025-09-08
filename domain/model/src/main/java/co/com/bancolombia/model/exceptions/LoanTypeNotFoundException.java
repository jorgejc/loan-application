package co.com.bancolombia.model.exceptions;

public class LoanTypeNotFoundException extends RuntimeException {
    public LoanTypeNotFoundException(Integer id) {
        super("Loan type with id " + id + " does not exist");
    }
}
