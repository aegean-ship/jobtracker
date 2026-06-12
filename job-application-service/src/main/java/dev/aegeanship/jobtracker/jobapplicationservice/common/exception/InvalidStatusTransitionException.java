package dev.aegeanship.jobtracker.jobapplicationservice.common.exception;

import dev.aegeanship.jobtracker.common.exception.BaseException;
import org.springframework.http.HttpStatus;


public class InvalidStatusTransitionException extends BaseException {

    public InvalidStatusTransitionException(String fromStatus, String toStatus) {
        super("INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT,
                "Invalid status transition: " + fromStatus + " -> " + toStatus);
    }
}
