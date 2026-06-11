package dev.aegeanship.jobtracker.jobapplicationservice.interview.service;

import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.application.service.JobApplicationService;
import dev.aegeanship.jobtracker.jobapplicationservice.common.exception.InvalidStatusTransitionException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.response.InterviewResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.entity.Interview;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewType;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.exception.InterviewNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.mapper.InterviewMapperImpl;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.repository.InterviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID INTERVIEW_ID = UUID.randomUUID();

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private JobApplicationService jobApplicationService;

    private InterviewService service;

    @BeforeEach
    void setUp() {
        service = new InterviewService(
                interviewRepository,
                jobApplicationService,
                new InterviewMapperImpl());
    }

    @Test
    void createResolvesOwnedApplicationAndDefaultsToScheduled() {
        when(jobApplicationService.getOwnedApplication(USER_ID, APPLICATION_ID))
                .thenReturn(jobApplication());

        InterviewResponse response = service.create(USER_ID, createRequest());

        assertThat(response.jobApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.type()).isEqualTo(InterviewType.TECHNICAL);
        assertThat(response.status()).isEqualTo(InterviewStatus.SCHEDULED);

        ArgumentCaptor<Interview> captor = ArgumentCaptor.forClass(Interview.class);
        verify(interviewRepository).save(captor.capture());
        assertThat(captor.getValue().getJobApplication().getId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void createPropagatesNotFoundForForeignApplication() {
        when(jobApplicationService.getOwnedApplication(USER_ID, APPLICATION_ID))
                .thenThrow(new JobApplicationNotFoundException(APPLICATION_ID.toString()));

        assertThatThrownBy(() -> service.create(USER_ID, createRequest()))
                .isInstanceOf(JobApplicationNotFoundException.class);
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenInterviewMissingOrNotOwned() {
        when(interviewRepository.findByIdAndJobApplication_UserId(INTERVIEW_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(USER_ID, INTERVIEW_ID))
                .isInstanceOf(InterviewNotFoundException.class);
    }

    @Test
    void getAllByApplicationChecksOwnershipAndMapsInterviews() {
        when(jobApplicationService.getOwnedApplication(USER_ID, APPLICATION_ID))
                .thenReturn(jobApplication());
        when(interviewRepository.findAllByJobApplicationIdOrderByScheduledAtAsc(APPLICATION_ID))
                .thenReturn(List.of(interview(InterviewStatus.SCHEDULED, null)));

        List<InterviewResponse> responses = service.getAllByApplication(USER_ID, APPLICATION_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().id()).isEqualTo(INTERVIEW_ID);
        assertThat(responses.getFirst().jobApplicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    void updateStatusAppendsStampedNote() {
        Interview interview = interview(InterviewStatus.SCHEDULED, null);
        when(interviewRepository.findByIdAndJobApplication_UserId(INTERVIEW_ID, USER_ID))
                .thenReturn(Optional.of(interview));

        InterviewResponse response = service.updateStatus(USER_ID, INTERVIEW_ID,
                new InterviewStatusUpdateRequest(InterviewStatus.COMPLETED, "went well"));

        assertThat(response.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(interview.getNotes()).isEqualTo("[SCHEDULED -> COMPLETED] went well");
    }

    @Test
    void updateStatusAppendsNoteToExistingNotes() {
        Interview interview = interview(InterviewStatus.SCHEDULED, "prep done");
        when(interviewRepository.findByIdAndJobApplication_UserId(INTERVIEW_ID, USER_ID))
                .thenReturn(Optional.of(interview));

        service.updateStatus(USER_ID, INTERVIEW_ID,
                new InterviewStatusUpdateRequest(InterviewStatus.COMPLETED, "went well"));

        assertThat(interview.getNotes())
                .isEqualTo("prep done\n[SCHEDULED -> COMPLETED] went well");
    }

    @Test
    void updateStatusLeavesNotesUntouchedForBlankNote() {
        Interview interview = interview(InterviewStatus.SCHEDULED, "prep done");
        when(interviewRepository.findByIdAndJobApplication_UserId(INTERVIEW_ID, USER_ID))
                .thenReturn(Optional.of(interview));

        service.updateStatus(USER_ID, INTERVIEW_ID,
                new InterviewStatusUpdateRequest(InterviewStatus.CANCELLED, "  "));

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.CANCELLED);
        assertThat(interview.getNotes()).isEqualTo("prep done");
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        Interview interview = interview(InterviewStatus.COMPLETED, null);
        when(interviewRepository.findByIdAndJobApplication_UserId(INTERVIEW_ID, USER_ID))
                .thenReturn(Optional.of(interview));

        assertThatThrownBy(() -> service.updateStatus(USER_ID, INTERVIEW_ID,
                new InterviewStatusUpdateRequest(InterviewStatus.CANCELLED, null)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(interview.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
    }

    @Test
    void deleteRemovesOwnedInterview() {
        Interview interview = interview(InterviewStatus.SCHEDULED, null);
        when(interviewRepository.findByIdAndJobApplication_UserId(INTERVIEW_ID, USER_ID))
                .thenReturn(Optional.of(interview));

        service.delete(USER_ID, INTERVIEW_ID);

        verify(interviewRepository).delete(interview);
    }

    private InterviewCreateRequest createRequest() {
        return new InterviewCreateRequest(
                APPLICATION_ID, InterviewType.TECHNICAL, null, 1, 60, null, null, null);
    }

    private JobApplication jobApplication() {
        JobApplication application = JobApplication.builder()
                .userId(USER_ID)
                .companyName("Acme")
                .positionTitle("Backend Engineer")
                .build();
        ReflectionTestUtils.setField(application, "id", APPLICATION_ID);
        return application;
    }

    private Interview interview(InterviewStatus status, String notes) {
        Interview interview = Interview.builder()
                .jobApplication(jobApplication())
                .type(InterviewType.TECHNICAL)
                .status(status)
                .notes(notes)
                .build();
        ReflectionTestUtils.setField(interview, "id", INTERVIEW_ID);
        return interview;
    }
}
