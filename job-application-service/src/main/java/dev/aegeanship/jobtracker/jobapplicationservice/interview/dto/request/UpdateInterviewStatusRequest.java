package dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInterviewStatusRequest(

        @NotNull(message = "Target status is required")
        InterviewStatus toStatus,

        String note
) {
}
