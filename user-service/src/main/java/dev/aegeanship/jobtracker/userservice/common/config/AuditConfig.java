package dev.aegeanship.jobtracker.userservice.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        // TODO: replace with SecurityContextHolder lookup once Spring Security is set up
        // Example:
        // return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        //         .filter(Authentication::isAuthenticated)
        //         .map(Authentication::getName);
        return () -> Optional.of("system");
    }
}
