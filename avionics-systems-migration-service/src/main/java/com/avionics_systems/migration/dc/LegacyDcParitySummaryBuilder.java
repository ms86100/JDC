package com.avionics_systems.migration.dc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds post-import parity summary for UI and reports.
 */
public final class LegacyDcParitySummaryBuilder {

    private LegacyDcParitySummaryBuilder() {
    }

    public static Map<String, Object> build(
            int entitiesExpected,
            int processed,
            int failed,
            int historyReplayed,
            int incrementalSkipped,
            long attachmentBytes,
            int attachmentCount,
            int referenceCatalogSize,
            String format,
            Integer riskScore,
            Map<String, Integer> processedByType,
            boolean historyOnlyImport,
            boolean stubDownstream) {

        int succeeded = Math.max(0, processed - failed);
        double coveragePct = entitiesExpected > 0
                ? Math.min(100.0, (succeeded * 100.0) / entitiesExpected)
                : 0.0;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("entitiesExpected", entitiesExpected);
        summary.put("entitiesProcessed", processed);
        summary.put("entitiesSucceeded", succeeded);
        summary.put("entitiesFailed", failed);
        summary.put("coveragePercent", Math.round(coveragePct * 10) / 10.0);
        summary.put("historyReplayed", historyReplayed);
        summary.put("incrementalSkipped", incrementalSkipped);
        summary.put("attachmentBytesWritten", attachmentBytes);
        summary.put("attachmentsCompleted", attachmentCount);
        summary.put("referenceCatalogSize", referenceCatalogSize);
        summary.put("format", format);
        summary.put("riskScore", riskScore);
        summary.put("historyOnlyImport", historyOnlyImport);
        summary.put("stubDownstream", stubDownstream);
        if (processedByType != null) {
            summary.put("processedByType", new HashMap<>(processedByType));
        }
        summary.put("parityStatus", coveragePct >= 95 && failed == 0 ? "PASS" : failed == 0 ? "WARN" : "FAIL");
        return summary;
    }
}
