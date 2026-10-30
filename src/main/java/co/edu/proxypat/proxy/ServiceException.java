package co.edu.proxypat.proxy;

/** Falla de una operación ya registrada en el log, con su requestId para correlacionar. */
public class ServiceException extends RuntimeException {

    private final String requestId;

    public ServiceException(String message, String requestId, Throwable cause) {
        super(message, cause);
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}
