package dev.aegeanship.jobtracker.jobapplicationservice.interview.repository;

import dev.aegeanship.jobtracker.jobapplicationservice.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findAllByJobApplicationIdOrderByScheduledAtAsc(UUID jobApplicationId);

    Optional<Interview> findByIdAndJobApplication_UserId(UUID id, UUID userId);
}
