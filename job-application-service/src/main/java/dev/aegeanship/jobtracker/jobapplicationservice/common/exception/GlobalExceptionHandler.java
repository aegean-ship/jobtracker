package dev.aegeanship.jobtracker.jobapplicationservice.common.exception;


import dev.aegeanship.jobtracker.common.exception.BaseException;
import dev.aegeanship.jobtracker.common.response.ApiError;
import dev.aegeanship.jobtracker.common.response.ApiStandardResponse;
import dev.aegeanship.jobtracker.common.response.ValidationFieldError;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.exception.InterviewNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

@Slf4j
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

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleInvalidStatusTransitionException(
            InvalidStatusTransitionException ex,
            HttpServletRequest request) {
        return buildErrorResponse(ex, request);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleBaseException(
            BaseException ex,
            HttpServletRequest request) {
        return buildErrorResponse(ex, request);
    }

    // missing X-User-Id header or query parameter: the framework message
    // names the missing input and contains nothing internal
    @ExceptionHandler({
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiStandardResponse<Void>> handleMissingInput(
            Exception ex,
            HttpServletRequest request) {
        return badRequest(ex, ex.getMessage(), request);
    }

    // malformed path/query values (e.g. UUIDs): name the parameter but not
    // the converter internals the framework message would expose
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return badRequest(ex, "Parameter '%s' has an invalid value".formatted(ex.getName()),
                request);
    }

    // unreadable JSON: Jackson messages leak class names, accepted enum
    // values and parser positions, so the client gets a fixed message
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiStandardResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        return badRequest(ex, "Request body is malformed or contains invalid values", request);
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


    private ResponseEntity<ApiStandardResponse<Void>> badRequest(
            Exception ex,
            String clientMessage,
            HttpServletRequest request) {

        String requestId = generateRequestId();
        // the original message is logged, not returned, so details stay server-side
        log.warn("[{}] {} {} -> 400 BAD_REQUEST: {}",
                requestId, request.getMethod(), request.getRequestURI(), ex.getMessage());

        ApiError error = ApiError.simple(
                requestId,
                request.getRequestURI(),
                "BAD_REQUEST",
                clientMessage
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
