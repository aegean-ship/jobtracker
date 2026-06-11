package dev.aegeanship.jobtracker.userservice.user.repository;

import dev.aegeanship.jobtracker.userservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;



@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

}
