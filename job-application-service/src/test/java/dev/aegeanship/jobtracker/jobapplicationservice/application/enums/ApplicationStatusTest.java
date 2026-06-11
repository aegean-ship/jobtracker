package dev.aegeanship.jobtracker.jobapplicationservice.application.enums;

import org.junit.jupiter.api.Test;

import static dev.aegeanship.jobtracker.jobapplicationservice.application.enums.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @Test
    void terminalStatusesAllowNoTransitions() {
        for (ApplicationStatus terminal : new ApplicationStatus[]{ACCEPTED, REJECTED, WITHDRAWN}) {
            for (ApplicationStatus target : values()) {
                assertThat(terminal.canTransitionTo(target))
                        .as("%s -> %s", terminal, target)
                        .isFalse();
            }
        }
    }

    @Test
    void noStatusTransitionsToItself() {
        for (ApplicationStatus status : values()) {
            assertThat(status.canTransitionTo(status))
                    .as("%s -> %s", status, status)
                    .isFalse();
        }
    }

    @Test
    void typicalLifecycleIsAllowed() {
        assertThat(SAVED.canTransitionTo(APPLIED)).isTrue();
        assertThat(APPLIED.canTransitionTo(SCREENING)).isTrue();
        assertThat(SCREENING.canTransitionTo(INTERVIEWING)).isTrue();
        assertThat(INTERVIEWING.canTransitionTo(OFFER)).isTrue();
        assertThat(OFFER.canTransitionTo(ACCEPTED)).isTrue();
    }

    @Test
    void ghostedApplicationCanBeRevived() {
        assertThat(INTERVIEWING.canTransitionTo(GHOSTED)).isTrue();
        assertThat(GHOSTED.canTransitionTo(INTERVIEWING)).isTrue();
        assertThat(GHOSTED.canTransitionTo(OFFER)).isTrue();
    }

    @Test
    void backwardAndSkippingShortcutsAreRejected() {
        assertThat(SAVED.canTransitionTo(OFFER)).isFalse();
        assertThat(OFFER.canTransitionTo(SAVED)).isFalse();
        assertThat(SCREENING.canTransitionTo(APPLIED)).isFalse();
        assertThat(GHOSTED.canTransitionTo(ACCEPTED)).isFalse();
    }
}
