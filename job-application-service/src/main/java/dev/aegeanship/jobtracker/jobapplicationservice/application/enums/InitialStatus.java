package dev.aegeanship.jobtracker.jobapplicationservice.application.enums;

/**
 * The states a job application may be created in: bookmarked for later
 * (SAVED) or already submitted (APPLIED). A deliberate subset of
 * {@link ApplicationStatus} so requests can never start mid-lifecycle.
 */
public enum InitialStatus {
    SAVED,
    APPLIED;

    public ApplicationStatus toApplicationStatus() {
        return this == SAVED ? ApplicationStatus.SAVED : ApplicationStatus.APPLIED;
    }
}
