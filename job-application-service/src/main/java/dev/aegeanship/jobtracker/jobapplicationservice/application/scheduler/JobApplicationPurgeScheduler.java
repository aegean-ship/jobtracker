package dev.aegeanship.jobtracker.jobapplicationservice.application.scheduler;

import dev.aegeanship.jobtracker.jobapplicationservice.application.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Permanently removes job applications whose soft-delete grace period
 * has expired. Status history and interviews go with them via the
 * ON DELETE CASCADE constraints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobApplicationPurgeScheduler {

    private final JobApplicationRepository jobApplicationRepository;

    @Value("${jobtracker.cleanup.retention}")
    private Duration retention;

    @Scheduled(cron = "${jobtracker.cleanup.cron}")
    @Transactional
    public void purgeExpiredApplications() {
        Instant cutoff = Instant.now().minus(retention);
        int purged = jobApplicationRepository.deleteAllSoftDeletedBefore(cutoff);
        if (purged > 0) {
            log.info("Purged {} job application(s) soft-deleted before {}", purged, cutoff);
        }
    }
}
