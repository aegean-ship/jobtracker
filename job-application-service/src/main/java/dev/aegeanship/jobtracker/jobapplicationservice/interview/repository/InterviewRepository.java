package dev.aegeanship.jobtracker.jobapplicationservice.interview.repository;

import dev.aegeanship.jobtracker.jobapplicationservice.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, UUID> {

}
