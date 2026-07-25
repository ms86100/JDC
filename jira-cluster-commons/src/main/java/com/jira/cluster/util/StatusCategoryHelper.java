package com.jira.cluster.util;

/**
 * Pure-Java utility for classifying issue status names into
 * high-level status categories (DONE, IN_PROGRESS, TODO).
 *
 * <p>This class is intentionally <em>not</em> a Spring component.
 * All methods are static and thread-safe.</p>
 */
public final class StatusCategoryHelper {

    private StatusCategoryHelper() {
        // utility class — prevent instantiation
    }

    /**
     * Returns {@code true} when the given status name indicates a
     * completed/resolved state (case-insensitive substring match).
     *
     * @param statusName the human-readable status name (may be {@code null})
     * @return {@code true} if the status is considered "done"
     */
    public static boolean isCompleted(String statusName) {
        if (statusName == null) {
            return false;
        }
        String lower = statusName.toLowerCase();
        return lower.contains("done")
                || lower.contains("closed")
                || lower.contains("resolved")
                || lower.contains("completed");
    }

    /**
     * Returns {@code true} when the given status name indicates an
     * active / in-progress state (case-insensitive substring match).
     *
     * @param statusName the human-readable status name (may be {@code null})
     * @return {@code true} if the status is considered "in progress"
     */
    public static boolean isInProgress(String statusName) {
        if (statusName == null) {
            return false;
        }
        String lower = statusName.toLowerCase();
        return lower.contains("progress")
                || lower.contains("review")
                || lower.contains("doing");
    }

    /**
     * Maps a status name to one of three category strings:
     * {@code "DONE"}, {@code "IN_PROGRESS"}, or {@code "TODO"}.
     *
     * <p>Evaluation order: completed is checked first, then in-progress,
     * and everything else (including {@code null}) falls through to TODO.</p>
     *
     * @param statusName the human-readable status name (may be {@code null})
     * @return the category string, never {@code null}
     */
    public static String getCategory(String statusName) {
        if (isCompleted(statusName)) {
            return "DONE";
        }
        if (isInProgress(statusName)) {
            return "IN_PROGRESS";
        }
        return "TODO";
    }
}
