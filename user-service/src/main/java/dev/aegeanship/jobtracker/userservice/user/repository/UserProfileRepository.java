package dev.aegeanship.jobtracker.userservice.user.repository;

import dev.aegeanship.jobtracker.userservice.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
