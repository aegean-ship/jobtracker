package dev.aegeanship.jobtracker.userservice.user.entity;

import dev.aegeanship.jobtracker.common.entity.BaseEntity;
import dev.aegeanship.jobtracker.userservice.user.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * Represents a registered user in the system.
 * Holds authentication credentials, personal info,
 * and role assignment for access control.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {


    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    @Email(regexp = ".+@.+\\..+", message = "Email format is invalid")
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String phoneNumber;

    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


}