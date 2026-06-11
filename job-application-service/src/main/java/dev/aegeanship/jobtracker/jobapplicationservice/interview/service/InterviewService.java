package dev.aegeanship.jobtracker.jobapplicationservice.interview.service;

import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import dev.aegeanship.jobtracker.jobapplicationservice.application.service.JobApplicationService;
import dev.aegeanship.jobtracker.jobapplicationservice.common.exception.InvalidStatusTransitionException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.request.InterviewStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.dto.response.InterviewResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.entity.Interview;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.exception.InterviewNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.mapper.InterviewMapper;
import dev.aegeanship.jobtracker.jobapplicationservice.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application logic for interviews. Ownership is enforced through the
 * parent job application; an interview belonging to another user's
 * application is reported as not found.
 */
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationService jobApplicationService;
    private final InterviewMapper interviewMapper;

    @Transactional
    public InterviewResponse create(UUID userId, InterviewCreateRequest request) {
        JobApplication application =
                jobApplicationService.getOwnedApplication(userId, request.jobApplicationId());

        Interview interview = interviewMapper.toEntity(request, application);
        interviewRepository.save(interview);

        return interviewMapper.toResponse(interview);
    }

    @Transactional(readOnly = true)
    public InterviewResponse getById(UUID userId, UUID id) {
        return interviewMapper.toResponse(getOwnedInterview(userId, id));
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getAllByApplication(UUID userId, UUID jobApplicationId) {
        JobApplication application =
                jobApplicationService.getOwnedApplication(userId, jobApplicationId);

        return interviewRepository
                .findAllByJobApplicationIdOrderByScheduledAtAsc(application.getId())
                .stream()
                .map(interviewMapper::toResponse)
                .toList();
    }

    @Transactional
    public InterviewResponse updateStatus(UUID userId, UUID id,
                                          InterviewStatusUpdateRequest request) {
        Interview interview = getOwnedInterview(userId, id);
        InterviewStatus fromStatus = interview.getStatus();
        InterviewStatus toStatus = request.toStatus();

        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new InvalidStatusTransitionException(fromStatus.name(), toStatus.name());
        }

        interview.setStatus(toStatus);
        appendTransitionNote(interview, fromStatus, toStatus, request.note());

        return interviewMapper.toResponse(interview);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        interviewRepository.delete(getOwnedInterview(userId, id));
    }

    private Interview getOwnedInterview(UUID userId, UUID id) {
        return interviewRepository.findByIdAndJobApplication_UserId(id, userId)
                .orElseThrow(() -> new InterviewNotFoundException(id.toString()));
    }

    /**
     * Interviews keep no per-transition history table, so a status update
     * note is appended to the free-form notes, stamped with the transition.
     */
    private void appendTransitionNote(Interview interview, InterviewStatus fromStatus,
                                      InterviewStatus toStatus, String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        String stamped = "[%s -> %s] %s".formatted(fromStatus, toStatus, note);
        interview.setNotes(interview.getNotes() == null
                ? stamped
                : interview.getNotes() + "\n" + stamped);
    }
}
