package dev.aegeanship.jobtracker.userservice.common.response;

public record ValidationFieldError(

        String field ,

        String message
) {
}
