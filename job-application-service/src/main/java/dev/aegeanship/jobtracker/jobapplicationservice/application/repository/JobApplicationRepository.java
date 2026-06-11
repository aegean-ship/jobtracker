package dev.aegeanship.jobtracker.jobapplicationservice.application.repository;

import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;



@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Page<JobApplication> findAllByUserId(UUID userId, Pageable pageable);

    Optional<JobApplication> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Permanently removes applications soft-deleted before the cutoff.
     * Native query: @SQLRestriction hides soft-deleted rows from JPQL,
     * and child rows are removed by the ON DELETE CASCADE constraints.
     */
    @Modifying
    @Query(value = "DELETE FROM job_applications WHERE deleted_at < :cutoff", nativeQuery = true)
    int deleteAllSoftDeletedBefore(@Param("cutoff") Instant cutoff);
}
