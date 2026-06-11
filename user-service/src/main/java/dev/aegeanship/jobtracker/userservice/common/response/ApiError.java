package dev.aegeanship.jobtracker.userservice.common.response;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(

        String requestId ,

        String urn ,

        Instant timestamp ,

        String code ,

        String message ,

        List<ValidationFieldError> fieldErrors

) {



    public static ApiError simple(
            String requestId,
            String endpointUrn,
            String code,
            String message
    ) {
        return new ApiError(
                requestId,
                endpointUrn,
                Instant.now(),
                code,
                message,
                null
        );
    }

    public static ApiError withFieldErrors(
            String requestId,
            String urn,
            String code,
            String message,
            List<ValidationFieldError> fieldErrors
    ) {
        return new ApiError(requestId, urn, Instant.now(), code, message, fieldErrors);
    }

}
