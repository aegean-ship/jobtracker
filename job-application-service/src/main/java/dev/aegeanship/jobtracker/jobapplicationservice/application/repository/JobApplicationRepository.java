package dev.aegeanship.jobtracker.jobapplicationservice.application.repository;

import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;



@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Page<JobApplication> findAllByUserId(UUID userId, Pageable pageable);

    Optional<JobApplication> findByIdAndUserId(UUID id, UUID userId);
}
