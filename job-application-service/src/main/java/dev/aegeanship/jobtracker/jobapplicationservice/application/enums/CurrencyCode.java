package dev.aegeanship.jobtracker.jobapplicationservice.application.enums;

/**
 * Supported salary currencies, as ISO 4217 codes.
 * Adding a constant is backward-compatible; removing one breaks
 * deserialization of existing rows, so extend on demand only.
 */
public enum CurrencyCode {
    TRY,
    USD,
    EUR,
    GBP
}
