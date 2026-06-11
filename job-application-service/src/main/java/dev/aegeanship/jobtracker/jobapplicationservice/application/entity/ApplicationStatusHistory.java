package dev.aegeanship.jobtracker.jobapplicationservice.application.entity;

import dev.aegeanship.jobtracker.common.entity.BaseEntity;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Records a status transition of a job application.
 * The transition timestamp is provided by BaseEntity.createdAt.
 * fromStatus is null for the initial status of an application.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "application_status_history")
public class ApplicationStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus toStatus;

    private String note;
}
