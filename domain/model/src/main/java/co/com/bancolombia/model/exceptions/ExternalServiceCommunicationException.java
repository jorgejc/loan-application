package co.com.bancolombia.model.exceptions;

public class ExternalServiceCommunicationException extends RuntimeException {
    private final String service;
    private final String endpoint;

    public ExternalServiceCommunicationException(String message) {
        super(message);
        this.service = null;
        this.endpoint = null;
    }

    public ExternalServiceCommunicationException(String service, String endpoint, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
        this.endpoint = endpoint;
    }

    public String getService() { return service; }
    public String getEndpoint() { return endpoint; }
}
