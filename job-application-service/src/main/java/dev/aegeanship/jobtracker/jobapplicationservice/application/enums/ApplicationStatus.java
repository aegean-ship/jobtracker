package dev.aegeanship.jobtracker.jobapplicationservice.application.enums;

/**
 * Lifecycle states of a job application,
 * from initial bookmarking through final outcome.
 */
public enum ApplicationStatus {
    SAVED,
    APPLIED,
    SCREENING,
    INTERVIEWING,
    OFFER,
    ACCEPTED,
    REJECTED,
    WITHDRAWN,
    GHOSTED;

    /**
     * Returns whether this status may transition to the given target.
     * ACCEPTED, REJECTED and WITHDRAWN are terminal; GHOSTED may be
     * revived if the company responds after all.
     */
    public boolean canTransitionTo(ApplicationStatus target) {
        return switch (this) {
            case SAVED -> target == APPLIED || target == WITHDRAWN;
            case APPLIED -> target == SCREENING || target == INTERVIEWING || target == OFFER
                    || target == REJECTED || target == WITHDRAWN || target == GHOSTED;
            case SCREENING -> target == INTERVIEWING || target == OFFER
                    || target == REJECTED || target == WITHDRAWN || target == GHOSTED;
            case INTERVIEWING -> target == OFFER
                    || target == REJECTED || target == WITHDRAWN || target == GHOSTED;
            case OFFER -> target == ACCEPTED || target == REJECTED || target == WITHDRAWN;
            case GHOSTED -> target == SCREENING || target == INTERVIEWING || target == OFFER
                    || target == REJECTED;
            case ACCEPTED, REJECTED, WITHDRAWN -> false;
        };
    }
}
