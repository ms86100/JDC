package com.jira.migration.dc;

/**
 * Enterprise SLA targets for Jira DC issue import (parse vs live job).
 */
public final class JiraDcImportSlaPolicy {

    /** SAX parse-only: 10k issues must finish within this budget (unit gate). */
    public static final long PARSE_10K_MAX_MS = 120_000L;

    /** Live import job: 10k issues must complete within 30 minutes. */
    public static final long LIVE_IMPORT_10K_MAX_MS = 1_800_000L;

    /** Live import job: 1k issues smoke SLA (5 minutes). */
    public static final long LIVE_IMPORT_1K_MAX_MS = 300_000L;

    /** Minimum issues for full 10k SLA tier. */
    public static final int TIER_10K_ISSUES = 10_000;

    /** Minimum issues for 1k smoke SLA tier. */
    public static final int TIER_1K_ISSUES = 1_000;

    private JiraDcImportSlaPolicy() {
    }

    public static long maxDurationMsForIssueCount(int issueCount) {
        if (issueCount >= TIER_10K_ISSUES) {
            return LIVE_IMPORT_10K_MAX_MS;
        }
        if (issueCount >= TIER_1K_ISSUES) {
            return LIVE_IMPORT_1K_MAX_MS;
        }
        // Small batches: 60s per 100 issues, minimum 120s
        return Math.max(120_000L, (issueCount / 100L + 1) * 60_000L);
    }
}
