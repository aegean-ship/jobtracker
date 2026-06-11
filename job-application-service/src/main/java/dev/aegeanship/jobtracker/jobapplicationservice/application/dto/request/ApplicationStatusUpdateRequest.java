package dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationStatusUpdateRequest(

        @NotNull(message = "Target status is required")
        ApplicationStatus toStatus,

        String note
) {
}
