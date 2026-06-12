package dev.aegeanship.jobtracker.jobapplicationservice.interview.enums;

/**
 * Scheduling/outcome state of an interview.
 */
public enum InterviewStatus {
    SCHEDULED,
    RESCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /**
     * Returns whether this status may transition to the given target.
     * COMPLETED, CANCELLED and NO_SHOW are terminal.
     */
    public boolean canTransitionTo(InterviewStatus target) {
        return switch (this) {
            case SCHEDULED, RESCHEDULED -> target == RESCHEDULED || target == COMPLETED
                    || target == CANCELLED || target == NO_SHOW;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }
}
