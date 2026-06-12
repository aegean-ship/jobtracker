package dev.aegeanship.jobtracker.jobapplicationservice.application.service;

import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.ApplicationStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.ApplicationStatusHistoryResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.JobApplicationResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.ApplicationStatusHistory;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.InitialStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.application.mapper.ApplicationStatusHistoryMapperImpl;
import dev.aegeanship.jobtracker.jobapplicationservice.application.mapper.JobApplicationMapperImpl;
import dev.aegeanship.jobtracker.jobapplicationservice.application.repository.ApplicationStatusHistoryRepository;
import dev.aegeanship.jobtracker.jobapplicationservice.application.repository.JobApplicationRepository;
import dev.aegeanship.jobtracker.jobapplicationservice.common.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(
                jobApplicationRepository,
                statusHistoryRepository,
                new JobApplicationMapperImpl(),
                new ApplicationStatusHistoryMapperImpl());
    }

    @Test
    void createSavesApplicationWithDefaultStatusAndInitialHistory() {
        JobApplicationCreateRequest request = createRequest();

        JobApplicationResponse response = service.create(USER_ID, request);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.companyName()).isEqualTo("Acme");
        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.appliedAt()).isEqualTo(LocalDate.now());

        verify(jobApplicationRepository).save(any(JobApplication.class));

        ApplicationStatusHistory history = capturedHistory();
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void createKeepsExplicitAppliedAtForBackfills() {
        LocalDate backfilledDate = LocalDate.now().minusDays(10);

        JobApplicationResponse response =
                service.create(USER_ID, createRequest(InitialStatus.APPLIED, backfilledDate));

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.appliedAt()).isEqualTo(backfilledDate);
    }

    @Test
    void createWithSavedStatusPersistsBookmark() {
        JobApplicationResponse response = service.create(USER_ID, createRequest(InitialStatus.SAVED));

        assertThat(response.status()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(response.appliedAt()).isNull();

        ApplicationStatusHistory history = capturedHistory();
        assertThat(history.getFromStatus()).isNull();
        assertThat(history.getToStatus()).isEqualTo(ApplicationStatus.SAVED);
    }

    @Test
    void getByIdReturnsOwnedApplication() {
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application(ApplicationStatus.APPLIED)));

        JobApplicationResponse response = service.getById(USER_ID, APPLICATION_ID);

        assertThat(response.id()).isEqualTo(APPLICATION_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
    }

    @Test
    void getByIdThrowsWhenApplicationMissingOrNotOwned() {
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(USER_ID, APPLICATION_ID))
                .isInstanceOf(JobApplicationNotFoundException.class);
    }

    @Test
    void getAllByUserMapsPage() {
        when(jobApplicationRepository.findAllByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(application(ApplicationStatus.APPLIED))));

        Page<JobApplicationResponse> page = service.getAllByUser(USER_ID, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().id()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void updateReplacesEditableFieldsAndPreservesLifecycleFields() {
        JobApplication application = application(ApplicationStatus.SCREENING);
        application.setAppliedAt(LocalDate.of(2026, 6, 1));
        application.setNotes("old notes");
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application));

        JobApplicationResponse response = service.update(USER_ID, APPLICATION_ID,
                new JobApplicationUpdateRequest("Globex", null, "Staff Engineer",
                        null, "Berlin", null, null, null, null, null, null));

        assertThat(response.companyName()).isEqualTo("Globex");
        assertThat(response.positionTitle()).isEqualTo("Staff Engineer");
        assertThat(application.getLocation()).isEqualTo("Berlin");
        // full replacement: a null in the request clears the field
        assertThat(application.getNotes()).isNull();
        // lifecycle fields stay untouched and no history is written
        assertThat(application.getUserId()).isEqualTo(USER_ID);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(application.getAppliedAt()).isEqualTo(LocalDate.of(2026, 6, 1));
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenApplicationMissingOrNotOwned() {
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(USER_ID, APPLICATION_ID,
                new JobApplicationUpdateRequest("Globex", null, "Staff Engineer",
                        null, null, null, null, null, null, null, null)))
                .isInstanceOf(JobApplicationNotFoundException.class);
    }

    @Test
    void updateStatusAppliesTransitionAndRecordsHistory() {
        JobApplication application = application(ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application));

        JobApplicationResponse response = service.updateStatus(USER_ID, APPLICATION_ID,
                new ApplicationStatusUpdateRequest(ApplicationStatus.SCREENING, "recruiter call"));

        assertThat(response.status()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SCREENING);

        ApplicationStatusHistory history = capturedHistory();
        assertThat(history.getFromStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(history.getToStatus()).isEqualTo(ApplicationStatus.SCREENING);
        assertThat(history.getNote()).isEqualTo("recruiter call");
    }

    @Test
    void updateStatusFromSavedToAppliedStampsAppliedAt() {
        JobApplication application = application(ApplicationStatus.SAVED);
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application));

        JobApplicationResponse response = service.updateStatus(USER_ID, APPLICATION_ID,
                new ApplicationStatusUpdateRequest(ApplicationStatus.APPLIED, null));

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(application.getAppliedAt()).isEqualTo(LocalDate.now());

        ApplicationStatusHistory history = capturedHistory();
        assertThat(history.getFromStatus()).isEqualTo(ApplicationStatus.SAVED);
        assertThat(history.getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        JobApplication application = application(ApplicationStatus.ACCEPTED);
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateStatus(USER_ID, APPLICATION_ID,
                new ApplicationStatusUpdateRequest(ApplicationStatus.APPLIED, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void getStatusHistoryReturnsMappedEntries() {
        JobApplication application = application(ApplicationStatus.SCREENING);
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application));
        when(statusHistoryRepository.findAllByJobApplicationIdOrderByCreatedAtAsc(APPLICATION_ID))
                .thenReturn(List.of(
                        history(application, null, ApplicationStatus.APPLIED, null),
                        history(application, ApplicationStatus.APPLIED, ApplicationStatus.SCREENING,
                                "recruiter call")));

        List<ApplicationStatusHistoryResponse> historyEntries =
                service.getStatusHistory(USER_ID, APPLICATION_ID);

        assertThat(historyEntries).hasSize(2);
        assertThat(historyEntries.getFirst().fromStatus()).isNull();
        assertThat(historyEntries.getFirst().toStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(historyEntries.getLast().jobApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(historyEntries.getLast().note()).isEqualTo("recruiter call");
    }

    @Test
    void deleteRemovesOwnedApplication() {
        JobApplication application = application(ApplicationStatus.APPLIED);
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(application));

        service.delete(USER_ID, APPLICATION_ID);

        verify(jobApplicationRepository).delete(application);
    }

    @Test
    void deleteThrowsWhenApplicationMissingOrNotOwned() {
        when(jobApplicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, APPLICATION_ID))
                .isInstanceOf(JobApplicationNotFoundException.class);
        verify(jobApplicationRepository, never()).delete(any());
    }

    private JobApplicationCreateRequest createRequest() {
        return createRequest(null);
    }

    private JobApplicationCreateRequest createRequest(InitialStatus status) {
        return createRequest(status, null);
    }

    private JobApplicationCreateRequest createRequest(InitialStatus status, LocalDate appliedAt) {
        return new JobApplicationCreateRequest(
                "Acme", null, "Backend Engineer", null, null, null,
                null, null, null, appliedAt, null, null, status);
    }

    private JobApplication application(ApplicationStatus status) {
        JobApplication application = JobApplication.builder()
                .userId(USER_ID)
                .companyName("Acme")
                .positionTitle("Backend Engineer")
                .status(status)
                .build();
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
        return application;
    }

    private ApplicationStatusHistory history(JobApplication application,
                                             ApplicationStatus fromStatus,
                                             ApplicationStatus toStatus,
                                             String note) {
        return ApplicationStatusHistory.builder()
                .jobApplication(application)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .note(note)
                .build();
    }

    private ApplicationStatusHistory capturedHistory() {
        ArgumentCaptor<ApplicationStatusHistory> captor =
                ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        return captor.getValue();
    }
}
