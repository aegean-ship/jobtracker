package dev.aegeanship.jobtracker.common.response;

public record ValidationFieldError(

        String field ,

        String message
) {
}
