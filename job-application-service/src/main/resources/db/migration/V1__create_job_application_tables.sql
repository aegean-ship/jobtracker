-- Initial schema for job-application-service:
-- job_applications, application_status_history, interviews.

CREATE TABLE job_applications
(
    id               UUID PRIMARY KEY,
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    created_by       VARCHAR(255)   NOT NULL,
    updated_by       VARCHAR(255)   NOT NULL,
    user_id          UUID           NOT NULL,
    company_name     VARCHAR(255)   NOT NULL,
    company_website  VARCHAR(255),
    position_title   VARCHAR(255)   NOT NULL,
    job_posting_url  VARCHAR(255),
    location         VARCHAR(255),
    work_mode        VARCHAR(255)
        CONSTRAINT chk_job_applications_work_mode
            CHECK (work_mode IN ('ONSITE', 'HYBRID', 'REMOTE')),
    salary_min       NUMERIC(38, 2),
    salary_max       NUMERIC(38, 2),
    currency         VARCHAR(3)
        CONSTRAINT chk_job_applications_currency
            CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP')),
    status           VARCHAR(255)   NOT NULL
        CONSTRAINT chk_job_applications_status
            CHECK (status IN ('SAVED', 'APPLIED', 'SCREENING', 'INTERVIEWING',
                              'OFFER', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'GHOSTED')),
    applied_at       DATE,
    source           VARCHAR(255),
    notes            TEXT,
    -- set on soft delete; rows past the grace period are purged by a scheduled job
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_job_applications_user_id ON job_applications (user_id)
    WHERE deleted_at IS NULL;

CREATE TABLE application_status_history
(
    id                 UUID PRIMARY KEY,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    updated_by         VARCHAR(255) NOT NULL,
    job_application_id UUID         NOT NULL
        CONSTRAINT fk_application_status_history_job_application
            REFERENCES job_applications (id) ON DELETE CASCADE,
    from_status        VARCHAR(255)
        CONSTRAINT chk_application_status_history_from_status
            CHECK (from_status IN ('SAVED', 'APPLIED', 'SCREENING', 'INTERVIEWING',
                                   'OFFER', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'GHOSTED')),
    to_status          VARCHAR(255) NOT NULL
        CONSTRAINT chk_application_status_history_to_status
            CHECK (to_status IN ('SAVED', 'APPLIED', 'SCREENING', 'INTERVIEWING',
                                 'OFFER', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'GHOSTED')),
    note               TEXT
);

CREATE INDEX idx_application_status_history_job_application_id
    ON application_status_history (job_application_id);

CREATE TABLE interviews
(
    id                 UUID PRIMARY KEY,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    created_by         VARCHAR(255) NOT NULL,
    updated_by         VARCHAR(255) NOT NULL,
    job_application_id UUID         NOT NULL
        CONSTRAINT fk_interviews_job_application
            REFERENCES job_applications (id) ON DELETE CASCADE,
    type               VARCHAR(255) NOT NULL
        CONSTRAINT chk_interviews_type
            CHECK (type IN ('PHONE_SCREEN', 'TECHNICAL', 'SYSTEM_DESIGN', 'BEHAVIORAL',
                            'HR', 'TAKE_HOME', 'ONSITE', 'FINAL')),
    status             VARCHAR(255) NOT NULL
        CONSTRAINT chk_interviews_status
            CHECK (status IN ('SCHEDULED', 'RESCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    round              INTEGER,
    scheduled_at       TIMESTAMPTZ,
    duration_minutes   INTEGER,
    interviewer_name   VARCHAR(255),
    meeting_link       VARCHAR(255),
    notes              TEXT
);

CREATE INDEX idx_interviews_job_application_id ON interviews (job_application_id);
