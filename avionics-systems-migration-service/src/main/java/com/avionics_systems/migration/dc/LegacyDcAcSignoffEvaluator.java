package com.avionics_systems.migration.dc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates enterprise AC-1..AC-10 from job metadata and import outcomes (honest — no auto-pass).
 */
public final class LegacyDcAcSignoffEvaluator {

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_NOT_RUN = "NOT_RUN";

    private LegacyDcAcSignoffEvaluator() {
    }

    public record AcSignoffItem(
            String id,
            String title,
            String status,
            String evidence,
            boolean signoffReady) {
    }

    /**
     * Pre-import AC preview from validate-upload response (no job yet).
     */
    public static Map<String, Object> evaluatePreImport(Map<String, Object> validateResult, Map<String, Object> options) {
        Map<String, Object> pseudoMeta = new LinkedHashMap<>();
        if (validateResult != null) {
            pseudoMeta.put("format", validateResult.get("format"));
            if (Boolean.TRUE.equals(validateResult.get("backupZipDetected"))) {
                pseudoMeta.put("backupZipDetected", true);
            }
            @SuppressWarnings("unchecked")
            Map<String, Long> byType = validateResult.get("entitiesByType") instanceof Map<?, ?> m
                    ? (Map<String, Long>) m : Map.of();
            pseudoMeta.put("processedByType", byType);
            int unknown = validateResult.get("unknownCustomFields") instanceof List<?> l ? l.size() : 0;
            if (unknown > 0) {
                pseudoMeta.put("unknownCustomFieldsResolved", false);
            }
        }
        Map<String, Object> opts = options != null ? options : Map.of();
        if (Boolean.TRUE.equals(validateResult != null ? validateResult.get("backupZipDetected") : null)) {
            opts = new LinkedHashMap<>(opts);
            opts.put("backupZip", true);
        }
        return evaluate(pseudoMeta, opts, "PREVIEW", 0, 0);
    }

    public static Map<String, Object> evaluate(
            Map<String, Object> resultMetadata,
            Map<String, Object> jobOptions,
            String jobStatus,
            int totalEntities,
            int failedEntities) {

        Map<String, Object> meta = resultMetadata != null ? resultMetadata : Map.of();
        Map<String, Object> opts = jobOptions != null ? jobOptions : Map.of();

        @SuppressWarnings("unchecked")
        Map<String, Object> parity = meta.get("paritySummary") instanceof Map<?, ?> p
                ? (Map<String, Object>) p : Map.of();
        @SuppressWarnings("unchecked")
        Map<String, Object> sla = meta.get("slaProof") instanceof Map<?, ?> s
                ? (Map<String, Object>) s : Map.of();

        List<AcSignoffItem> items = new ArrayList<>();
        items.add(ac1(opts, meta));
        items.add(ac2(sla));
        items.add(ac3(meta));
        items.add(ac4(meta));
        items.add(ac5(opts, meta));
        items.add(ac6(meta));
        items.add(ac7(meta, jobStatus, failedEntities));
        items.add(ac8());
        items.add(ac9(jobStatus, failedEntities));
        items.add(ac10(meta));

        long pass = items.stream().filter(i -> STATUS_PASS.equals(i.status())).count();
        long partial = items.stream().filter(i -> STATUS_PARTIAL.equals(i.status())).count();
        long ready = items.stream().filter(AcSignoffItem::signoffReady).count();

        List<Map<String, Object>> serialized = items.stream()
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", i.id());
                    m.put("title", i.title());
                    m.put("status", i.status());
                    m.put("evidence", i.evidence());
                    m.put("signoffReady", i.signoffReady());
                    return m;
                })
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("criteria", serialized);
        out.put("passCount", pass);
        out.put("partialCount", partial);
        out.put("failCount", items.size() - pass - partial);
        out.put("signoffReadyCount", ready);
        out.put("formalSignoffComplete", ready == items.size());
        out.put("overallStatus", ready == items.size() ? STATUS_PASS
                : pass + partial > 0 ? STATUS_PARTIAL : STATUS_FAIL);
        out.put("jobStatus", jobStatus);
        out.put("totalEntities", totalEntities);
        out.put("failedEntities", failedEntities);
        if (!parity.isEmpty()) {
            out.put("coveragePercent", parity.get("coveragePercent"));
        }
        return out;
    }

    private static AcSignoffItem ac1(Map<String, Object> opts, Map<String, Object> meta) {
        boolean backup = opts.get("extractedBackupRoot") != null
                || Boolean.TRUE.equals(opts.get("backupZip"));
        boolean pass = backup && meta.get("format") != null;
        return new AcSignoffItem(
                "AC-1",
                "Import DC 9.x backup ZIP from UI",
                pass ? STATUS_PARTIAL : STATUS_FAIL,
                backup ? "Backup ZIP path resolved (extractedBackupRoot or backupZip flag)"
                        : "No backup ZIP detected in job options",
                false);
    }

    private static AcSignoffItem ac2(Map<String, Object> sla) {
        if (sla.isEmpty()) {
            return new AcSignoffItem("AC-2", "10k issues < SLA", STATUS_NOT_RUN,
                    "No slaProof on job — run a non-stub live import", false);
        }
        boolean met = Boolean.TRUE.equals(sla.get("slaMet"));
        boolean stub = Boolean.TRUE.equals(sla.get("stubDownstream"));
        return new AcSignoffItem(
                "AC-2",
                "10k issues < SLA",
                met ? STATUS_PASS : stub ? STATUS_PARTIAL : STATUS_FAIL,
                "slaProof: tier=" + sla.get("slaTier") + ", durationMs=" + sla.get("durationMs")
                        + ", maxAllowedMs=" + sla.get("maxAllowedMs") + ", slaMet=" + sla.get("slaMet"),
                met);
    }

    private static AcSignoffItem ac3(Map<String, Object> meta) {
        Object rate = meta.get("attachmentChecksumMatchRate");
        if (rate instanceof Number n) {
            boolean ok = n.doubleValue() >= 99.9;
            return new AcSignoffItem("AC-3", "Attachment SHA-256 ≥99.9%", ok ? STATUS_PASS : STATUS_PARTIAL,
                    "Match rate " + n + "%", ok);
        }
        int att = meta.get("attachmentCount") instanceof Number n ? n.intValue() : 0;
        return new AcSignoffItem("AC-3", "Attachment SHA-256 ≥99.9%", att > 0 ? STATUS_PARTIAL : STATUS_FAIL,
                att > 0 ? att + " attachments written; checksum rate not recorded on job" : "No attachments imported",
                false);
    }

    private static AcSignoffItem ac4(Map<String, Object> meta) {
        int replayed = meta.get("historyReplayed") instanceof Number n ? n.intValue() : 0;
        return new AcSignoffItem("AC-4", "Changelog field-level match", replayed > 0 ? STATUS_PARTIAL : STATUS_FAIL,
                replayed > 0 ? "historyReplayed=" + replayed + " (not diff-verified against DC)" : "No history replayed",
                false);
    }

    private static AcSignoffItem ac5(Map<String, Object> opts, Map<String, Object> meta) {
        boolean historyOnly = Boolean.TRUE.equals(opts.get("historyOnlyImport"))
                || Boolean.TRUE.equals(meta.get("historyOnlyImport"));
        return new AcSignoffItem("AC-5", "No spurious transitions (history-only)", STATUS_PARTIAL,
                historyOnly ? "historyOnlyImport=true (issues may still be created unless dry-run)"
                        : "Standard import mode",
                false);
    }

    private static AcSignoffItem ac6(Map<String, Object> meta) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> byType = meta.get("processedByType") instanceof Map<?, ?> m
                ? (Map<String, Integer>) m : Map.of();
        boolean plugin = byType.getOrDefault("PluginEntity", 0) > 0
                || Boolean.TRUE.equals(meta.get("unknownCustomFieldsResolved"));
        return new AcSignoffItem("AC-6", "Plugin CF mapped or registry", plugin ? STATUS_PARTIAL : STATUS_FAIL,
                plugin ? "Plugin entities processed or unknown CF registry used" : "No plugin/unknown CF evidence",
                false);
    }

    private static AcSignoffItem ac7(Map<String, Object> meta, String jobStatus, int failed) {
        boolean proven = Boolean.TRUE.equals(meta.get("rollbackProven"));
        if (proven) {
            return new AcSignoffItem("AC-7", "Rollback proven", STATUS_PASS,
                    "rollbackProven=true after successful rollback on this job", true);
        }
        if ("PREVIEW".equals(jobStatus)) {
            return new AcSignoffItem("AC-7", "Rollback proven", STATUS_NOT_RUN,
                    "Run import then rollback drill to record rollbackProven on job metadata", false);
        }
        return new AcSignoffItem("AC-7", "Rollback proven", STATUS_FAIL,
                "Rollback API exists; invoke rollback on a completed job to prove AC-7",
                false);
    }

    private static AcSignoffItem ac8() {
        return new AcSignoffItem("AC-8", "All operations in UI", STATUS_PARTIAL,
                "Validate, import, parity, conflicts, rollback panels wired; not every AC op verified",
                false);
    }

    private static AcSignoffItem ac9(String jobStatus, int failed) {
        boolean ok = "COMPLETED".equals(jobStatus) && failed == 0;
        return new AcSignoffItem("AC-9", "E2E suite green", ok ? STATUS_PARTIAL : STATUS_FAIL,
                ok ? "Job completed with zero failures" : "Job failed or incomplete — run Playwright MIGRATION_E2E_FULL",
                false);
    }

    private static AcSignoffItem ac10(Map<String, Object> meta) {
        return new AcSignoffItem("AC-10", "Security review", STATUS_PARTIAL,
                "XXE/zip-slip/bomb caps in code; formal security sign-off not attached",
                false);
    }
}
