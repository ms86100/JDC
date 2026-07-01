package com.jira.migration.dc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records whether a completed import job met live-import SLA for its issue volume.
 */
public final class JiraDcImportSlaProofBuilder {

    private JiraDcImportSlaProofBuilder() {
    }

    public static Map<String, Object> build(
            int issueCount,
            long durationMs,
            int failedEntities,
            boolean stubDownstream,
            String proofType) {

        long maxAllowedMs = JiraDcImportSlaPolicy.maxDurationMsForIssueCount(issueCount);
        boolean slaMet = issueCount > 0
                && failedEntities == 0
                && durationMs > 0
                && durationMs <= maxAllowedMs
                && !stubDownstream;

        double issuesPerSecond = durationMs > 0 ? (issueCount * 1000.0) / durationMs : 0.0;

        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("proofType", proofType != null ? proofType : "LIVE_IMPORT_JOB");
        proof.put("issueCount", issueCount);
        proof.put("durationMs", durationMs);
        proof.put("maxAllowedMs", maxAllowedMs);
        proof.put("issuesPerSecond", Math.round(issuesPerSecond * 100.0) / 100.0);
        proof.put("slaMet", slaMet);
        proof.put("stubDownstream", stubDownstream);
        proof.put("failedEntities", failedEntities);
        proof.put("slaTier", issueCount >= JiraDcImportSlaPolicy.TIER_10K_ISSUES ? "10K"
                : issueCount >= JiraDcImportSlaPolicy.TIER_1K_ISSUES ? "1K" : "SMALL");
        if (stubDownstream) {
            proof.put("slaNote", "SLA not counted: stubDownstream skips real issue-service writes");
        } else if (!slaMet && issueCount > 0) {
            proof.put("slaNote", "Job exceeded max duration for tier or had failures");
        }
        return proof;
    }

    /** Parse-only SLA proof (SAX path, no persist). */
    public static Map<String, Object> buildParseProof(int issueCount, long parseDurationMs) {
        boolean slaMet = issueCount > 0 && parseDurationMs <= JiraDcImportSlaPolicy.PARSE_10K_MAX_MS;
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("proofType", "PARSE_ONLY");
        proof.put("issueCount", issueCount);
        proof.put("durationMs", parseDurationMs);
        proof.put("maxAllowedMs", JiraDcImportSlaPolicy.PARSE_10K_MAX_MS);
        proof.put("slaMet", slaMet);
        proof.put("stubDownstream", false);
        return proof;
    }
}
