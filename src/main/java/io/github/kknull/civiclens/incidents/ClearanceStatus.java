package io.github.kknull.civiclens.incidents;

/**
 * Represents the normalized clearance states used by CivicLens.
 *
 * <p>External source codes are translated into these domain values
 * before persistence.</p>
 */
public enum ClearanceStatus {
    CLEARED_BY_ARREST,
    CLEARED_BY_EXCEPTION,
    NOT_CLEARED
}
