package dev.aegeanship.jobtracker.jobapplicationservice.application.entity;

import dev.aegeanship.jobtracker.common.entity.BaseEntity;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.WorkMode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a single job application submitted (or saved) by a user.
 * The user is referenced by id only; user data lives in user-service.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "job_applications", indexes = {
        @Index(name = "idx_job_applications_user_id", columnList = "user_id")
})
public class JobApplication extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String companyName;

    private String companyWebsite;

    @Column(nullable = false)
    private String positionTitle;

    private String jobPostingUrl;

    private String location;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private LocalDate appliedAt;

    private String source;

    @Column(columnDefinition = "text")
    private String notes;
}
