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
    GHOSTED
}
