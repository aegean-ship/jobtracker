package dev.aegeanship.jobtracker.jobapplicationservice.interview.exception;

import dev.aegeanship.jobtracker.common.exception.BaseException;
import org.springframework.http.HttpStatus;


public class InterviewNotFoundException extends BaseException {

    public InterviewNotFoundException(String identifier) {
        super("INTERVIEW_NOT_FOUND", HttpStatus.NOT_FOUND,
                "Interview not found: " + identifier);
    }
}
