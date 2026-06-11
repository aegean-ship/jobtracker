package dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response;

import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.WorkMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobApplicationResponse(
        UUID id,
        UUID userId,
        String companyName,
        String companyWebsite,
        String positionTitle,
        String jobPostingUrl,
        String location,
        WorkMode workMode,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        ApplicationStatus status,
        LocalDate appliedAt,
        String source,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
