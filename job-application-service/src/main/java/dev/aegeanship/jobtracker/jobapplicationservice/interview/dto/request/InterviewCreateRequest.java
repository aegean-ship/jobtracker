package dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request;

import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record InterviewCreateRequest(

        @NotNull(message = "Job application id is required")
        UUID jobApplicationId,

        @NotNull(message = "Interview type is required")
        InterviewType type,

        Instant scheduledAt,

        @Positive(message = "Round must be positive")
        Integer round,

        @Positive(message = "Duration must be positive")
        Integer durationMinutes,

        String interviewerName,

        String meetingLink,

        String notes
) {
}
