package dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response;

import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusHistoryResponse(
        UUID id,
        UUID jobApplicationId,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String note,
        Instant createdAt
) {
}
