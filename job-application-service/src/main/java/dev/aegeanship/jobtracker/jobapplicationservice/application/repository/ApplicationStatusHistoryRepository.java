package dev.aegeanship.jobtracker.jobapplicationservice.application.repository;

import dev.aegeanship.jobtracker.jobapplicationservice.application.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;



@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

}
