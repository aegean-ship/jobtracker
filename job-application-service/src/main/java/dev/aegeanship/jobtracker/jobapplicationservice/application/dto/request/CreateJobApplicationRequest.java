package dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.WorkMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateJobApplicationRequest(

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

        String currency,

        LocalDate appliedAt,

        String source,

        String notes
) {
}
