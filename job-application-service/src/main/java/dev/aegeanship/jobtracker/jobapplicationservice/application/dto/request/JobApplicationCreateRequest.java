package dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.CurrencyCode;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.InitialStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.WorkMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobApplicationCreateRequest(

        @NotBlank(message = "Company name is required")
        String companyName,

        String companyWebsite,

        @NotBlank(message = "Position title is required")
        String positionTitle,

        String jobPostingUrl,

        String location,

        WorkMode workMode,

        @PositiveOrZero(message = "Minimum salary must be zero or positive")
        BigDecimal salaryMin,

        @PositiveOrZero(message = "Maximum salary must be zero or positive")
        BigDecimal salaryMax,

        CurrencyCode currency,

        LocalDate appliedAt,

        String source,

        String notes,

        // null means APPLIED; a SAVED application is a bookmark not yet applied to
        InitialStatus status
) {

    @AssertTrue(message = "appliedAt must not be set when status is SAVED")
    private boolean isAppliedAtConsistentWithStatus() {
        return status != InitialStatus.SAVED || appliedAt == null;
    }
}
