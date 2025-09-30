package examp.org.com.dbquerzapp.exception;

import com.example.model.ErrorDto;
import org.springdoc.api.OpenApiResourceNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;

@Order(Ordered.LOWEST_PRECEDENCE)
//@RestControllerAdvice(basePackages = "examp.org.com.dbquerzapp") // <-- scope to your controllers
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleBadRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, "Bad request: " + ex.getMessage());
    }

    // If you truly need a “not found” mapping, catch a specific exception type you throw,
    // e.g. ResourceNotFoundException, rather than IOException.
    @ExceptionHandler(OpenApiResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(OpenApiResourceNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "Resource not found: " + ex.getMessage());
    }

    // Preserve framework-set statuses (404/401/403/415/406/etc.)
    @ExceptionHandler({ ResponseStatusException.class, ErrorResponseException.class })
    public ResponseEntity<ErrorDto> handleWithOriginalStatus(Exception ex) {
        HttpStatus status = ex instanceof ResponseStatusException rse
                ? HttpStatus.valueOf(rse.getStatusCode().value())
                : HttpStatus.valueOf(((ErrorResponseException) ex).getStatusCode().value());
        return body(status, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleAny(Exception ex) {
        // Respect @ResponseStatus on the exception class, if present
        ResponseStatus ann = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        if (ann != null) {
            return body(ann.code(), ex.getMessage());
        }
        // Fallback to 500
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<ErrorDto> body(HttpStatus status, String msg) {
        ErrorDto error = new ErrorDto();
        error.setErrorCode(status.value());
        error.setMessage(msg);
        return ResponseEntity.status(status).body(error);
    }
}