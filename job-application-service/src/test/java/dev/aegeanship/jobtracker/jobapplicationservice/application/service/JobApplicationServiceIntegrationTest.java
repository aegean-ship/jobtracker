package dev.aegeanship.jobtracker.jobapplicationservice.application.service;

import dev.aegeanship.jobtracker.jobapplicationservice.AbstractIntegrationTest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.ApplicationStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.ApplicationStatusHistoryResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.JobApplicationResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.InitialStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.application.repository.JobApplicationRepository;
import dev.aegeanship.jobtracker.jobapplicationservice.application.scheduler.JobApplicationPurgeScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobApplicationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JobApplicationService service;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobApplicationPurgeScheduler purgeScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE job_applications CASCADE");
        userId = UUID.randomUUID();
    }

    @Test
    void createPersistsApplicationWithAuditFieldsAndInitialHistory() {
        JobApplicationResponse response = service.create(userId, createRequest("Acme"));

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        String createdBy = jdbcTemplate.queryForObject(
                "SELECT created_by FROM job_applications WHERE id = ?",
                String.class, response.id());
        assertThat(createdBy).isEqualTo("system");

        List<ApplicationStatusHistoryResponse> history =
                service.getStatusHistory(userId, response.id());
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().fromStatus()).isNull();
        assertThat(history.getFirst().toStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void savedBookmarkBecomesAppliedWithStampedDateAndHistoryTrail() {
        JobApplicationResponse bookmark =
                service.create(userId, createRequest("Acme", InitialStatus.SAVED));

        assertThat(bookmark.status()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(bookmark.appliedAt()).isNull();

        JobApplicationResponse applied = service.updateStatus(userId, bookmark.id(),
                new ApplicationStatusUpdateRequest(ApplicationStatus.APPLIED, "sent CV"));

        assertThat(applied.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(applied.appliedAt()).isEqualTo(LocalDate.now());

        List<ApplicationStatusHistoryResponse> history =
                service.getStatusHistory(userId, bookmark.id());
        assertThat(history).hasSize(2);
        assertThat(history.getFirst().fromStatus()).isNull();
        assertThat(history.getFirst().toStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(history.getLast().fromStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(history.getLast().toStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void getByIdEnforcesOwnership() {
        JobApplicationResponse created = service.create(userId, createRequest("Acme"));

        assertThat(service.getById(userId, created.id()).id()).isEqualTo(created.id());
        assertThatThrownBy(() -> service.getById(UUID.randomUUID(), created.id()))
                .isInstanceOf(JobApplicationNotFoundException.class);
    }

    @Test
    void getAllByUserPaginatesOwnApplicationsOnly() {
        service.create(userId, createRequest("Acme"));
        service.create(userId, createRequest("Globex"));
        service.create(userId, createRequest("Initech"));
        service.create(UUID.randomUUID(), createRequest("SomeoneElsesCorp"));

        Page<JobApplicationResponse> firstPage = service.getAllByUser(userId, PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void updatePersistsReplacedFieldsAndKeepsLifecycleFields() {
        JobApplicationResponse created = service.create(userId, createRequest("Acme"));

        service.update(userId, created.id(),
                new JobApplicationUpdateRequest("Globex", null, "Staff Engineer",
                        null, "Berlin", null, null, null, null, null, null));

        JobApplicationResponse reloaded = service.getById(userId, created.id());
        assertThat(reloaded.companyName()).isEqualTo("Globex");
        assertThat(reloaded.positionTitle()).isEqualTo("Staff Engineer");
        assertThat(reloaded.location()).isEqualTo("Berlin");
        assertThat(reloaded.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(service.getStatusHistory(userId, created.id())).hasSize(1);
    }

    @Test
    void updateStatusPersistsTransitionAndHistoryTrail() {
        JobApplicationResponse created = service.create(userId, createRequest("Acme"));

        service.updateStatus(userId, created.id(),
                new ApplicationStatusUpdateRequest(ApplicationStatus.SCREENING, "recruiter call"));

        assertThat(service.getById(userId, created.id()).status())
                .isEqualTo(ApplicationStatus.SCREENING);

        List<ApplicationStatusHistoryResponse> history =
                service.getStatusHistory(userId, created.id());
        assertThat(history).hasSize(2);
        assertThat(history.getLast().fromStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(history.getLast().toStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(history.getLast().note()).isEqualTo("recruiter call");
    }

    @Test
    void deleteSoftDeletesRowAndHidesItFromQueries() {
        JobApplicationResponse created = service.create(userId, createRequest("Acme"));

        service.delete(userId, created.id());

        assertThatThrownBy(() -> service.getById(userId, created.id()))
                .isInstanceOf(JobApplicationNotFoundException.class);
        assertThat(jobApplicationRepository.findById(created.id())).isEmpty();

        // the row itself is still there, marked deleted, history intact
        Instant deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM job_applications WHERE id = ?",
                Instant.class, created.id());
        assertThat(deletedAt).isNotNull();
        assertThat(countHistoryRows(created.id())).isEqualTo(1);
    }

    @Test
    void purgeHardDeletesOnlyApplicationsPastRetention() {
        JobApplicationResponse expired = service.create(userId, createRequest("Acme"));
        JobApplicationResponse recent = service.create(userId, createRequest("Globex"));
        service.delete(userId, expired.id());
        service.delete(userId, recent.id());

        // age the first soft delete beyond the 30d retention period
        jdbcTemplate.update("UPDATE job_applications SET deleted_at = ? WHERE id = ?",
                Instant.now().minus(31, ChronoUnit.DAYS).atOffset(ZoneOffset.UTC), expired.id());

        purgeScheduler.purgeExpiredApplications();

        assertThat(countApplicationRows(expired.id())).isZero();
        assertThat(countHistoryRows(expired.id())).isZero();
        assertThat(countApplicationRows(recent.id())).isEqualTo(1);
        assertThat(countHistoryRows(recent.id())).isEqualTo(1);
    }

    private int countApplicationRows(UUID id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM job_applications WHERE id = ?", Integer.class, id);
        return count == null ? 0 : count;
    }

    private int countHistoryRows(UUID jobApplicationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM application_status_history WHERE job_application_id = ?",
                Integer.class, jobApplicationId);
        return count == null ? 0 : count;
    }

    private JobApplicationCreateRequest createRequest(String companyName) {
        return createRequest(companyName, null);
    }

    private JobApplicationCreateRequest createRequest(String companyName, InitialStatus status) {
        return new JobApplicationCreateRequest(
                companyName, null, "Backend Engineer", null, null, null,
                null, null, null, null, null, null, status);
    }
}
