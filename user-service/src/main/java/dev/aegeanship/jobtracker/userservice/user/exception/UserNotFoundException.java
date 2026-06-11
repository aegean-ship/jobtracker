package dev.aegeanship.jobtracker.userservice.user.exception;

import dev.aegeanship.jobtracker.common.exception.BaseException;
import org.springframework.http.HttpStatus;


public class UserNotFoundException extends BaseException {

    public UserNotFoundException(String identifier) {
        super("USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                "User not found: " + identifier);
    }
}