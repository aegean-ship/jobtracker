package dev.aegeanship.jobtracker.jobapplicationservice.application.exception;

import dev.aegeanship.jobtracker.common.exception.BaseException;
import org.springframework.http.HttpStatus;


public class JobApplicationNotFoundException extends BaseException {

    public JobApplicationNotFoundException(String identifier) {
        super("JOB_APPLICATION_NOT_FOUND", HttpStatus.NOT_FOUND,
                "Job application not found: " + identifier);
    }
}
