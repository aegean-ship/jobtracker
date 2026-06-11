package dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.response;

import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;

import java.time.Instant;
import java.util.UUID;

public record InterviewResponse(
        UUID id,
        UUID jobApplicationId,
        InterviewType type,
        InterviewStatus status,
        Integer round,
        Instant scheduledAt,
        Integer durationMinutes,
        String interviewerName,
        String meetingLink,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
