package co.edu.proxypat.web;

import co.edu.proxypat.proxy.ServiceException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, Object>> serviceFailure(ServiceException failure) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "ERROR",
                "error", String.valueOf(failure.getMessage()),
                "requestId", failure.getRequestId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException failure) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "error", failure.getMessage()));
    }
}
