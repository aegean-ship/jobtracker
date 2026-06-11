package dev.aegeanship.jobtracker.userservice.user.entity.embeddable;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable class to hold social media links for a user profile.
 * This allows us to group related fields together and embed them in the UserProfile entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class SocialLinks {

    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String mediumUrl;
    private String hackerrankUrl;
    private String leetcodeUrl;
}
