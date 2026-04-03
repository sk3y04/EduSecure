package edusecure.edusecure.controller.common;

import edusecure.edusecure.dto.common.ValidationErrorResponse;
import edusecure.edusecure.service.auth.AuthApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

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
    @SuppressWarnings("unused")
    public ResponseEntity<ValidationErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        Throwable cause = ex.getMostSpecificCause();
        String message = cause != null
                && cause.getMessage() != null
                && cause.getMessage().toLowerCase().contains("boundary")
                ? "Multipart request is malformed or missing a valid boundary"
                : "Request body is malformed or contains invalid values";
        errors.put("_global", List.of(message));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(VALIDATION_FAILED_MESSAGE, errors));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ValidationErrorResponse> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put(ex.getRequestPartName(), List.of("Submission file is required"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse(VALIDATION_FAILED_MESSAGE, errors));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ValidationErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("file", List.of("Submission file exceeds the configured upload limit"));

        return ResponseEntity.status(HttpStatusCode.valueOf(413))
                .body(new ValidationErrorResponse("Upload too large", errors));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ValidationErrorResponse> handleMultipartException(MultipartException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("file", List.of("Submission upload must be sent as multipart/form-data"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse("Malformed multipart upload", errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ValidationErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("_global", List.of(ex.getReason() != null ? ex.getReason() : "Request failed"));

        return ResponseEntity.status(ex.getStatusCode())
                .body(new ValidationErrorResponse(ex.getReason() != null ? ex.getReason() : "Request failed", errors));
    }

    @ExceptionHandler(AuthApiException.class)
    public ResponseEntity<ValidationErrorResponse> handleAuthApiException(AuthApiException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        errors.put("_global", List.of(ex.getMessage()));

        return ResponseEntity.status(ex.getStatus())
                .body(new ValidationErrorResponse(ex.getMessage(), errors));
    }
}

