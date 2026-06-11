package dev.aegeanship.jobtracker.jobapplicationservice.common.exception;


import dev.aegeanship.jobtracker.common.exception.BaseException;
import dev.aegeanship.jobtracker.common.response.ApiError;
import dev.aegeanship.jobtracker.common.response.ApiStandardResponse;
import dev.aegeanship.jobtracker.common.response.ValidationFieldError;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.exception.InterviewNotFoundException;
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

    @ExceptionHandler(JobApplicationNotFoundException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleJobApplicationNotFoundException(
            JobApplicationNotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(ex, request);
    }

    @ExceptionHandler(InterviewNotFoundException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleInterviewNotFoundException(
            InterviewNotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(ex, request);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleBaseException(
            BaseException ex,
            HttpServletRequest request) {
        return buildErrorResponse(ex, request);
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


    private ResponseEntity<ApiStandardResponse<Void>> buildErrorResponse(
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

    private String generateRequestId() {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
