package dev.aegeanship.jobtracker.jobapplicationservice.interview.entity;

import dev.aegeanship.jobtracker.common.entity.BaseEntity;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Represents an interview stage belonging to a job application.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "interviews")
public class Interview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    private Integer round;

    private Instant scheduledAt;

    private Integer durationMinutes;

    private String interviewerName;

    private String meetingLink;

    @Column(columnDefinition = "text")
    private String notes;
}
