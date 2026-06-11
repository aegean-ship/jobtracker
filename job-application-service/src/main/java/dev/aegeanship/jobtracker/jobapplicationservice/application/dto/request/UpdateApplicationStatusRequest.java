package dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(

        @NotNull(message = "Target status is required")
        ApplicationStatus toStatus,

        String note
) {
}
