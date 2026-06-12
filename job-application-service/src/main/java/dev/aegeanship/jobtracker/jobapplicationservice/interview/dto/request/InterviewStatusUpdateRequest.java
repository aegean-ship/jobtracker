package dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;

public record InterviewStatusUpdateRequest(

        @NotNull(message = "Target status is required")
        InterviewStatus toStatus,

        String note
) {
}
