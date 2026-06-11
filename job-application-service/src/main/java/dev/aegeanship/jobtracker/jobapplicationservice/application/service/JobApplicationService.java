package dev.aegeanship.jobtracker.jobapplicationservice.application.service;

import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.ApplicationStatusUpdateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.request.JobApplicationCreateRequest;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.ApplicationStatusHistoryResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.dto.response.JobApplicationResponse;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.ApplicationStatusHistory;
import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus;
import dev.aegeanship.jobtracker.jobapplicationservice.application.exception.JobApplicationNotFoundException;
import dev.aegeanship.jobtracker.jobapplicationservice.application.mapper.ApplicationStatusHistoryMapper;
import dev.aegeanship.jobtracker.jobapplicationservice.application.mapper.JobApplicationMapper;
import dev.aegeanship.jobtracker.jobapplicationservice.application.repository.ApplicationStatusHistoryRepository;
import dev.aegeanship.jobtracker.jobapplicationservice.application.repository.JobApplicationRepository;
import dev.aegeanship.jobtracker.jobapplicationservice.common.exception.InvalidStatusTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application logic for job applications and their status history.
 * All operations are scoped to the owning user; an application that
 * exists but belongs to another user is reported as not found.
 */
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final JobApplicationMapper jobApplicationMapper;
    private final ApplicationStatusHistoryMapper statusHistoryMapper;

    @Transactional
    public JobApplicationResponse create(UUID userId, JobApplicationCreateRequest request) {
        JobApplication application = jobApplicationMapper.toEntity(request, userId);
        jobApplicationRepository.save(application);

        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .jobApplication(application)
                .toStatus(application.getStatus())
                .build());

        return jobApplicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse getById(UUID userId, UUID id) {
        return jobApplicationMapper.toResponse(getOwnedApplication(userId, id));
    }

    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> getAllByUser(UUID userId, Pageable pageable) {
        return jobApplicationRepository.findAllByUserId(userId, pageable)
                .map(jobApplicationMapper::toResponse);
    }

    @Transactional
    public JobApplicationResponse updateStatus(UUID userId, UUID id,
                                               ApplicationStatusUpdateRequest request) {
        JobApplication application = getOwnedApplication(userId, id);
        ApplicationStatus fromStatus = application.getStatus();
        ApplicationStatus toStatus = request.toStatus();

        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new InvalidStatusTransitionException(fromStatus.name(), toStatus.name());
        }

        application.setStatus(toStatus);
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .jobApplication(application)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .note(request.note())
                .build());

        return jobApplicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> getStatusHistory(UUID userId, UUID id) {
        JobApplication application = getOwnedApplication(userId, id);
        return statusHistoryRepository
                .findAllByJobApplicationIdOrderByCreatedAtAsc(application.getId())
                .stream()
                .map(statusHistoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        // soft delete via @SQLDelete; the purge scheduler hard-deletes the
        // row (and its children via ON DELETE CASCADE) after the retention period
        jobApplicationRepository.delete(getOwnedApplication(userId, id));
    }

    /**
     * Loads an application enforcing ownership, for use by services that
     * need the entity itself (e.g. InterviewService).
     */
    @Transactional(readOnly = true)
    public JobApplication getOwnedApplication(UUID userId, UUID id) {
        return jobApplicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new JobApplicationNotFoundException(id.toString()));
    }
}
