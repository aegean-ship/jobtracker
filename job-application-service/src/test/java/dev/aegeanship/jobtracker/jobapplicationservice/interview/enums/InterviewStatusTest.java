package dev.aegeanship.jobtracker.jobapplicationservice.interview.enums;

import org.junit.jupiter.api.Test;

import static dev.aegeanship.jobtracker.jobapplicationservice.interview.enums.InterviewStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class InterviewStatusTest {

    @Test
    void terminalStatusesAllowNoTransitions() {
        for (InterviewStatus terminal : new InterviewStatus[]{COMPLETED, CANCELLED, NO_SHOW}) {
            for (InterviewStatus target : values()) {
                assertThat(terminal.canTransitionTo(target))
                        .as("%s -> %s", terminal, target)
                        .isFalse();
            }
        }
    }

    @Test
    void openStatusesCanResolveOrReschedule() {
        for (InterviewStatus open : new InterviewStatus[]{SCHEDULED, RESCHEDULED}) {
            assertThat(open.canTransitionTo(RESCHEDULED)).as("%s -> RESCHEDULED", open).isTrue();
            assertThat(open.canTransitionTo(COMPLETED)).as("%s -> COMPLETED", open).isTrue();
            assertThat(open.canTransitionTo(CANCELLED)).as("%s -> CANCELLED", open).isTrue();
            assertThat(open.canTransitionTo(NO_SHOW)).as("%s -> NO_SHOW", open).isTrue();
        }
    }

    @Test
    void nothingTransitionsBackToScheduled() {
        for (InterviewStatus status : values()) {
            assertThat(status.canTransitionTo(SCHEDULED))
                    .as("%s -> SCHEDULED", status)
                    .isFalse();
        }
    }
}
