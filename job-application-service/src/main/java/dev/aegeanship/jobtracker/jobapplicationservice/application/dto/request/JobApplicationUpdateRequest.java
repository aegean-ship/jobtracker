package dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.CurrencyCode;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.WorkMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Full replacement of an application's editable fields. Lifecycle fields
 * are managed elsewhere: status through the status endpoint, appliedAt at
 * creation or stamped on the SAVED -> APPLIED transition.
 */
public record JobApplicationUpdateRequest(

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

        String source,

        String notes
) {
}
