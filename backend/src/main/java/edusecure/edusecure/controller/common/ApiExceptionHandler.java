package edusecure.edusecure.controller.common;

import edusecure.edusecure.dto.common.ValidationErrorResponse;
import edusecure.edusecure.service.auth.AuthApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String VALIDATION_FAILED_MESSAGE = "Validation failed";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fieldError.getField(), ignored -> new ArrayList<>());
            List<String> fieldMessages = errors.get(fieldError.getField());
            if (!fieldMessages.contains(fieldError.getDefaultMessage())) {
                fieldMessages.add(fieldError.getDefaultMessage());
            }
        }

        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            errors.computeIfAbsent("_global", ignored -> new ArrayList<>());
            List<String> globalMessages = errors.get("_global");
            if (!globalMessages.contains(globalError.getDefaultMessage())) {
                globalMessages.add(globalError.getDefaultMessage());
            }
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(VALIDATION_FAILED_MESSAGE, errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("_global", List.of("Request body is malformed or contains invalid values"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(VALIDATION_FAILED_MESSAGE, errors));
    }

    @ExceptionHandler(AuthApiException.class)
    public ResponseEntity<ValidationErrorResponse> handleAuthApiException(AuthApiException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("_global", List.of(ex.getMessage()));

        return ResponseEntity.status(ex.getStatus())
                .body(new ValidationErrorResponse(ex.getMessage(), errors));
    }
}

