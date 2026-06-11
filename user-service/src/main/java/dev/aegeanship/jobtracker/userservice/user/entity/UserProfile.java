package dev.aegeanship.jobtracker.userservice.user.entity;

import dev.aegeanship.jobtracker.common.entity.BaseEntity;
import dev.aegeanship.jobtracker.userservice.user.entity.embeddable.SocialLinks;
import jakarta.persistence.*;
import lombok.*;


/**
 * Represents optional profile information for a user.
 * Linked to User via one-to-one relationship.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {


    @OneToOne(fetch = FetchType.LAZY , optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String title;

    private String bio;

    @Embedded
    private SocialLinks socialLinks;


}
