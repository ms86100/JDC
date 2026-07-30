package com.avionics_systems.migration.dc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API-facing DC import validate/import helpers (used by controller + UI contract).
 */
@Service
@RequiredArgsConstructor
public class LegacyDcImportApiService {

    private final LegacyDcImportOrchestrator orchestrator;

    public Map<String, Object> validateUpload(
            Path xmlOrZipPath,
            boolean uploadedAsBackupZip,
            Path optionalAttachmentBundleZip,
            Map<String, Object> options) throws Exception {

        LegacyDcImportOrchestrator.ResolvedInputs resolved = orchestrator.resolveInputs(
                xmlOrZipPath, optionalAttachmentBundleZip, uploadedAsBackupZip);

        try {
            LegacyDcImportOrchestrator.PrepareResult prep = orchestrator.prepareValidate(
                    resolved.xmlPath(),
                    resolved.attachmentBundlePath(),
                    options != null ? options : Map.of());

            Map<String, Long> byType = prep.parseResult().getEntities().stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getEntityType(),
                            Collectors.counting()));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("valid", prep.validationReport().valid() && prep.parseResult().getTotalEntities() > 0);
            body.put("format", prep.parseResult().getXmlFormat() != null
                    ? prep.parseResult().getXmlFormat().name() : "UNKNOWN");
            body.put("totalEntities", prep.parseResult().getTotalEntities());
            body.put("entitiesByType", byType);
            body.put("riskScore", prep.validationReport().riskScore());
            body.put("blockerCount", prep.validationReport().blockerCount());
            body.put("warningCount", prep.validationReport().warningCount());
            body.put("relationshipEdges", prep.relationshipEdges());
            body.put("errors", prep.validationReport().errors().stream()
                    .map(e -> Map.of(
                            "field", e.getField(),
                            "code", e.getErrorCode(),
                            "message", e.getMessage()))
                    .toList());
            body.put("warnings", prep.validationReport().warnings().stream()
                    .map(w -> Map.of(
                            "field", w.getField(),
                            "code", w.getWarningCode(),
                            "message", w.getMessage()))
                    .toList());
            body.put("message", buildValidateMessage(prep));
            body.put("attachmentsRootResolved", resolved.attachmentBundlePath() != null
                    && Files.isDirectory(resolved.attachmentBundlePath()));
            body.put("backupZipDetected", uploadedAsBackupZip || resolved.extractedBackup() != null);
            body.put("conflicts", buildConflicts(prep));
            body.put("unknownCustomFields", extractUnknownFields(prep));
            body.put("acSignoffPreview", LegacyDcAcSignoffEvaluator.evaluatePreImport(body, options != null ? options : Map.of()));
            return body;
        } finally {
            cleanup(resolved);
        }
    }

    private static String buildValidateMessage(LegacyDcImportOrchestrator.PrepareResult prep) {
        if (prep.parseResult().getTotalEntities() == 0) {
            return "No importable entities detected";
        }
        if (!prep.validationReport().valid()) {
            return "Validation failed: " + prep.validationReport().blockerCount() + " blocker(s)";
        }
        return "Validation passed (" + prep.parseResult().getXmlFormat() + ", "
                + prep.parseResult().getTotalEntities() + " entities)";
    }

    private static List<Map<String, Object>> buildConflicts(LegacyDcImportOrchestrator.PrepareResult prep) {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (var e : prep.validationReport().errors()) {
            conflicts.add(Map.of(
                    "severity", "BLOCKER",
                    "code", e.getErrorCode() != null ? e.getErrorCode() : "ERROR",
                    "field", e.getField() != null ? e.getField() : "",
                    "entityKey", e.getMessage() != null ? e.getMessage() : "",
                    "message", e.getMessage() != null ? e.getMessage() : "",
                    "resolution", "BLOCK_IMPORT"));
        }
        for (var w : prep.validationReport().warnings()) {
            conflicts.add(Map.of(
                    "severity", "WARNING",
                    "code", w.getWarningCode() != null ? w.getWarningCode() : "WARN",
                    "field", w.getField() != null ? w.getField() : "",
                    "entityKey", w.getMessage() != null ? w.getMessage() : "",
                    "message", w.getMessage() != null ? w.getMessage() : "",
                    "resolution", "REVIEW"));
        }
        return conflicts;
    }

    private static List<Map<String, Object>> extractUnknownFields(LegacyDcImportOrchestrator.PrepareResult prep) {
        return prep.validationReport().warnings().stream()
                .filter(w -> "UNKNOWN_CUSTOM_FIELD".equals(w.getWarningCode()))
                .map(w -> Map.<String, Object>of(
                        "fieldId", w.getField() != null ? w.getField() : "",
                        "message", w.getMessage() != null ? w.getMessage() : ""))
                .toList();
    }

    private void cleanup(LegacyDcImportOrchestrator.ResolvedInputs resolved) {
        if (resolved.extractedBackup() != null) {
            orchestrator.cleanupExtracted(resolved.extractedBackup());
        }
    }
}
