package dev.aegeanship.jobtracker.jobapplicationservice.interview.service;

import dev.aegeanship.jobtracker.jobapplicationservice.AbstractIntegrationTest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.application.service.JobApplicationService;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.response.InterviewResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.exception.InterviewNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID applicationId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE job_applications CASCADE");
        userId = UUID.randomUUID();
        applicationId = jobApplicationService.create(userId, new JobApplicationCreateRequest(
                "Acme", null, "Backend Engineer", null, null, null,
                null, null, null, null, null, null)).id();
    }

    @Test
    void createPersistsInterviewLinkedToApplication() {
        InterviewResponse response = interviewService.create(userId, createRequest());

        assertThat(response.id()).isNotNull();
        assertThat(response.jobApplicationId()).isEqualTo(applicationId);
        assertThat(response.status()).isEqualTo(InterviewStatus.SCHEDULED);

        assertThat(interviewService.getAllByApplication(userId, applicationId))
                .extracting(InterviewResponse::id)
                .containsExactly(response.id());
    }

    @Test
    void createRejectsForeignApplication() {
        assertThatThrownBy(() -> interviewService.create(UUID.randomUUID(), createRequest()))
                .isInstanceOf(JobApplicationNotFoundException.class);
    }

    @Test
    void getByIdEnforcesOwnershipThroughParentApplication() {
        InterviewResponse created = interviewService.create(userId, createRequest());

        assertThat(interviewService.getById(userId, created.id()).id()).isEqualTo(created.id());
        assertThatThrownBy(() -> interviewService.getById(UUID.randomUUID(), created.id()))
                .isInstanceOf(InterviewNotFoundException.class);
    }

    @Test
    void interviewsOfSoftDeletedApplicationAreHidden() {
        InterviewResponse created = interviewService.create(userId, createRequest());

        jobApplicationService.delete(userId, applicationId);

        // @SQLRestriction on the parent filters the join, so the interview
        // disappears with its soft-deleted application
        assertThatThrownBy(() -> interviewService.getById(userId, created.id()))
                .isInstanceOf(InterviewNotFoundException.class);
        assertThatThrownBy(() -> interviewService.getAllByApplication(userId, applicationId))
                .isInstanceOf(JobApplicationNotFoundException.class);
    }

    @Test
    void updateStatusPersistsTransitionAndStampedNote() {
        InterviewResponse created = interviewService.create(userId, createRequest());

        interviewService.updateStatus(userId, created.id(),
                new InterviewStatusUpdateRequest(InterviewStatus.COMPLETED, "went well"));

        InterviewResponse reloaded = interviewService.getById(userId, created.id());
        assertThat(reloaded.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(reloaded.notes()).isEqualTo("[SCHEDULED -> COMPLETED] went well");
    }

    @Test
    void deleteRemovesInterviewButKeepsApplication() {
        InterviewResponse created = interviewService.create(userId, createRequest());

        interviewService.delete(userId, created.id());

        assertThat(interviewService.getAllByApplication(userId, applicationId)).isEmpty();
        assertThat(jobApplicationService.getById(userId, applicationId)).isNotNull();
    }

    private InterviewCreateRequest createRequest() {
        return new InterviewCreateRequest(
                applicationId, InterviewType.TECHNICAL, null, 1, 60, null, null, null);
    }
}
