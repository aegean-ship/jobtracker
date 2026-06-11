package dev.aegeanship.jobtracker.userservice.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public abstract class BaseException extends RuntimeException {


    private final HttpStatus status;

    private final String code;

    protected BaseException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    protected BaseException(String code, HttpStatus status, String message,
                            Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }


}
