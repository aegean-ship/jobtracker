package dev.aegeanship.jobtracker.userservice.common.exception;


import dev.aegeanship.jobtracker.common.exception.BaseException;
import dev.aegeanship.jobtracker.common.response.ApiError;
import dev.aegeanship.jobtracker.common.response.ApiStandardResponse;
import dev.aegeanship.jobtracker.common.response.ValidationFieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // handles all your domain exceptions (UserNotFoundException, etc.)
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleBaseException(
            BaseException ex,
            HttpServletRequest request) {

        ApiError error = ApiError.simple(
                generateRequestId(),
                request.getRequestURI(),
                ex.getCode(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiStandardResponse.error(error));
    }

    // handles @Valid / @Validated failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ValidationFieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> new ValidationFieldError(e.getField(), e.getDefaultMessage()))
                .toList();

        ApiError error = ApiError.withFieldErrors(
                generateRequestId(),
                request.getRequestURI(),
                "VALIDATION_FAILED",
                "Request validation failed",
                fieldErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiStandardResponse.error(error));
    }



    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
