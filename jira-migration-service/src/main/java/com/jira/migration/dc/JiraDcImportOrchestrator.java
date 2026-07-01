package com.jira.migration.dc;

import com.jira.migration.parser.JiraDcXmlFormat;
import com.jira.migration.parser.JiraDcXmlFormatDetector;
import com.jira.migration.parser.JiraDcXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * End-to-end DC XML import pipeline: parse → stage → validate → (optional) persist handoff.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraDcImportOrchestrator {

    private static final int DETECT_SNIPPET_BYTES = 8192;

    private final JiraDcXmlParser xmlParser;
    private final JiraDcStagingService stagingService;
    private final JiraDcImportValidationService validationService;
    private final JiraDcCustomFieldResolver customFieldResolver;
    private final JiraDcBackupZipHandler backupZipHandler;

    public PrepareResult prepare(
            UUID jobId,
            Path xmlPath,
            Path attachmentBundlePath,
            Map<String, Object> options,
            UUID sessionId) throws IOException {
        return prepare(jobId, xmlPath, attachmentBundlePath, options, sessionId, true);
    }

    public PrepareResult prepareValidate(
            Path xmlPath,
            Path attachmentBundlePath,
            Map<String, Object> options) throws IOException {
        return prepare(null, xmlPath, attachmentBundlePath, options, null, false);
    }

    private PrepareResult prepare(
            UUID jobId,
            Path xmlPath,
            Path attachmentBundlePath,
            Map<String, Object> options,
            UUID sessionId,
            boolean persistStaging) throws IOException {

        String detectSnippet = readDetectSnippet(xmlPath);
        JiraDcXmlFormat detectedFormat = JiraDcXmlFormatDetector.detect(detectSnippet);
        UUID parseJobId = jobId != null ? jobId : UUID.randomUUID();

        JiraDcXmlParser.ParseResult parseResult;
        if (xmlPath != null && (detectedFormat == JiraDcXmlFormat.ENTITIES_XML
                || detectedFormat == JiraDcXmlFormat.RSS_092)) {
            parseResult = xmlParser.parseXmlBackup(detectSnippet, parseJobId, xmlPath);
        } else if (Files.size(xmlPath) > 16 * 1024 * 1024) {
            throw new IOException("XML file exceeds 16MB in-memory limit; use entities.xml or RSS path streaming");
        } else {
            String xmlContent = Files.readString(xmlPath, StandardCharsets.UTF_8);
            parseResult = xmlParser.parseXmlBackup(xmlContent, parseJobId, xmlPath);
        }

        applyCustomFieldResolution(parseResult.getEntities());

        UUID batchId = persistStaging && jobId != null
                ? stagingService.stageParsedEntities(jobId, parseResult.getEntities(), detectSnippet)
                : null;

        JiraDcImportValidationService.PathAttachmentContext attCtx =
                attachmentBundlePath != null
                        ? new JiraDcImportValidationService.PathAttachmentContext(attachmentBundlePath)
                        : null;

        boolean clearPrevious = options == null || !Boolean.FALSE.equals(options.get("clearValidationResults"));
        JiraDcImportValidationService.JiraDcValidationReport report = validationService.validate(
                jobId, sessionId, parseResult.getEntities(), attCtx, clearPrevious);

        if (jobId != null) {
            stagingService.markValidationStates(jobId, report.entityKeyToState());
        }

        boolean dryRun = options != null && Boolean.TRUE.equals(options.get("dryRun"));
        boolean blockOnErrors = options != null && Boolean.TRUE.equals(options.get("blockOnValidationErrors"));
        boolean blocked = blockOnErrors && !report.valid();

        List<JiraDcXmlParser.ParsedEntity> importable = filterImportable(parseResult.getEntities(), report);
        JiraDcConflictResolutionApplier.applyFieldOverrides(importable, options);
        importable = JiraDcConflictResolutionApplier.applySkipEntities(importable, options);

        List<Map<String, String>> relationshipEdges = extractRelationshipEdges(parseResult.getEntities());

        return new PrepareResult(
                parseResult,
                batchId,
                report,
                dryRun,
                blocked,
                importable,
                attachmentBundlePath,
                relationshipEdges
        );
    }

    /**
     * Resolves XML path and attachment root from uploaded files (standalone XML, backup ZIP, or bundle ZIP).
     */
    public ResolvedInputs resolveInputs(Path xmlOrZipPath, Path optionalBundleZip, boolean isBackupZip)
            throws IOException {
        if (isBackupZip) {
            JiraDcBackupZipHandler.ExtractedBackup extracted = backupZipHandler.extractZipToTemp(xmlOrZipPath);
            Path att = extracted.attachmentsRoot();
            if (!Files.isDirectory(att)) {
                att = extracted.extractRoot();
            }
            return new ResolvedInputs(extracted.entitiesXml(), att, extracted);
        }
        JiraDcBackupZipHandler.ExtractedBackup bundleExtracted = null;
        Path attRoot = null;
        if (optionalBundleZip != null) {
            bundleExtracted = backupZipHandler.extractZipToTemp(optionalBundleZip);
            attRoot = bundleExtracted.attachmentsRoot();
            if (!Files.isDirectory(attRoot)) {
                attRoot = bundleExtracted.extractRoot();
            }
        }
        return new ResolvedInputs(xmlOrZipPath, attRoot, bundleExtracted);
    }

    private static List<Map<String, String>> extractRelationshipEdges(List<JiraDcXmlParser.ParsedEntity> entities) {
        List<Map<String, String>> edges = new ArrayList<>();
        Map<String, String> issueKeys = new HashMap<>();
        for (JiraDcXmlParser.ParsedEntity e : entities) {
            if ("Issue".equals(e.getEntityType()) || "SubTask".equals(e.getEntityType())) {
                issueKeys.put(e.getEntityKey(), e.getEntityKey());
            }
        }
        for (JiraDcXmlParser.ParsedEntity e : entities) {
            if ("Issue".equals(e.getEntityType()) || "SubTask".equals(e.getEntityType())) {
                Map<String, String> f = e.getFields();
                if (f == null) {
                    continue;
                }
                String parent = f.get("parent");
                if (parent != null) {
                    edges.add(Map.of("from", parent, "to", e.getEntityKey(), "type", "parent"));
                }
                String epic = f.get("epicLink");
                if (epic != null) {
                    edges.add(Map.of("from", epic, "to", e.getEntityKey(), "type", "epic"));
                }
            }
            if ("IssueLink".equals(e.getEntityType())) {
                Map<String, String> f = e.getFields();
                if (f != null) {
                    edges.add(Map.of(
                            "from", f.getOrDefault("sourceIssueKey", "?"),
                            "to", f.getOrDefault("targetIssueKey", "?"),
                            "type", f.getOrDefault("linkType", "link")));
                }
            }
        }
        return edges;
    }

    public record ResolvedInputs(
            Path xmlPath,
            Path attachmentBundlePath,
            JiraDcBackupZipHandler.ExtractedBackup extractedBackup
    ) {
    }

    private void applyCustomFieldResolution(List<JiraDcXmlParser.ParsedEntity> entities) {
        for (JiraDcXmlParser.ParsedEntity entity : entities) {
            if (!"Issue".equals(entity.getEntityType()) && !"SubTask".equals(entity.getEntityType())) {
                continue;
            }
            Map<String, String> fields = entity.getFields();
            if (fields == null) {
                continue;
            }
            Map<String, Object> resolved = customFieldResolver.resolve(fields);
            if (!resolved.isEmpty()) {
                fields.put("_resolvedCustomFields", resolved.toString());
            }
            String epic = resolved.get("epicLink") != null ? resolved.get("epicLink").toString() : null;
            if (epic != null) {
                fields.put("epicLink", epic);
            }
        }
    }

    private List<JiraDcXmlParser.ParsedEntity> filterImportable(
            List<JiraDcXmlParser.ParsedEntity> entities,
            JiraDcImportValidationService.JiraDcValidationReport report) {
        return entities.stream()
                .filter(e -> {
                    String state = report.entityKeyToState().getOrDefault(e.getEntityKey(), "VALID");
                    return !"BLOCKED".equals(state);
                })
                .toList();
    }

    public void cleanupExtracted(JiraDcBackupZipHandler.ExtractedBackup backup) {
        backupZipHandler.deleteExtracted(backup);
    }

    public void cleanupExtractedRoot(Path extractRoot) {
        if (extractRoot != null) {
            backupZipHandler.deleteExtracted(
                    new JiraDcBackupZipHandler.ExtractedBackup(extractRoot, null, null));
        }
    }

    private static String readDetectSnippet(Path xmlPath) throws IOException {
        byte[] bytes = Files.readAllBytes(xmlPath);
        int len = Math.min(bytes.length, DETECT_SNIPPET_BYTES);
        return new String(bytes, 0, len, StandardCharsets.UTF_8);
    }

    public record PrepareResult(
            JiraDcXmlParser.ParseResult parseResult,
            UUID stagingBatchId,
            JiraDcImportValidationService.JiraDcValidationReport validationReport,
            boolean dryRun,
            boolean blocked,
            List<JiraDcXmlParser.ParsedEntity> importableEntities,
            Path attachmentBundlePath,
            List<Map<String, String>> relationshipEdges
    ) {
    }
}
