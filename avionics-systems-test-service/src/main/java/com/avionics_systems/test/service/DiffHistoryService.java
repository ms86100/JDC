package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.DiffHistoryResponse;
import com.avionics_systems.test.dto.DiffHistoryResponse.FieldDiff;
import com.avionics_systems.test.entity.TechEvent;
import com.avionics_systems.test.entity.VvoDefinition;
import com.avionics_systems.test.repository.TechEventRepository;
import com.avionics_systems.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for comparing entity field values between two versions to produce diff reports.
 * <p>
 * Supports two modes:
 * - "difference" mode: Net delta between two entity versions (original vs cloned or compared entity).
 * - "evolution" mode: If a clone chain exists (via cloneSourceId), shows all intermediate changes
 *   from the oldest ancestor to the current entity.
 * <p>
 * Since the test-service does not have ChangeGroup/ChangeItem tables (those exist in issue-service),
 * this implementation compares current entity state by reflection: it compares two records by ID
 * (e.g., original vs cloned version) and highlights differences.
 * <p>
 * Generates an HTML report with color-coded changes:
 * - Green for additions
 * - Red for deletions
 * - Yellow/amber for modifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiffHistoryService {

    private final VvoDefinitionRepository vvoRepo;
    private final TechEventRepository techEventRepo;

    // Fields to skip when comparing (internal/technical fields)
    private static final List<String> SKIP_FIELDS = List.of(
            "id", "createdAt", "updatedAt", "createdBy", "cloneSourceId"
    );

    /**
     * Compare two VVO records and produce a diff report.
     *
     * @param entityId       The primary VVO id (typically the newer version)
     * @param comparedWithId The VVO id to compare against (typically the older version or clone source)
     * @param mode           "difference" for net delta, "evolution" for chain traversal
     */
    @Transactional(readOnly = true)
    public DiffHistoryResponse compareVvos(UUID entityId, UUID comparedWithId, String mode) {
        log.info("Comparing VVOs: {} vs {} (mode={})", entityId, comparedWithId, mode);

        VvoDefinition current = vvoRepo.findById(entityId)
                .orElseThrow(() -> new RuntimeException("VVO not found: " + entityId));

        if ("evolution".equalsIgnoreCase(mode)) {
            return buildEvolutionReport(current, "VVO");
        }

        VvoDefinition baseline = vvoRepo.findById(comparedWithId)
                .orElseThrow(() -> new RuntimeException("VVO not found: " + comparedWithId));

        List<FieldDiff> diffs = compareEntities(baseline, current);

        String html = generateHtmlReport(diffs, "VVO", baseline.getIssueKey(), current.getIssueKey());

        return DiffHistoryResponse.builder()
                .entityId(entityId)
                .comparedWithId(comparedWithId)
                .entityType("VVO")
                .mode("difference")
                .diffs(diffs)
                .htmlReport(html)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Compare two TechEvent records and produce a diff report.
     */
    @Transactional(readOnly = true)
    public DiffHistoryResponse compareTechEvents(UUID entityId, UUID comparedWithId, String mode) {
        log.info("Comparing TechEvents: {} vs {} (mode={})", entityId, comparedWithId, mode);

        TechEvent current = techEventRepo.findById(entityId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + entityId));

        if ("evolution".equalsIgnoreCase(mode)) {
            return buildTechEventEvolutionReport(current);
        }

        TechEvent baseline = techEventRepo.findById(comparedWithId)
                .orElseThrow(() -> new RuntimeException("TechEvent not found: " + comparedWithId));

        List<FieldDiff> diffs = compareEntities(baseline, current);

        String html = generateHtmlReport(diffs, "TechEvent", baseline.getIssueKey(), current.getIssueKey());

        return DiffHistoryResponse.builder()
                .entityId(entityId)
                .comparedWithId(comparedWithId)
                .entityType("TechEvent")
                .mode("difference")
                .diffs(diffs)
                .htmlReport(html)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Build an evolution report for a VVO by walking the clone chain.
     * Follows cloneSourceId back to the original VVO and compiles diffs at each step.
     */
    private DiffHistoryResponse buildEvolutionReport(VvoDefinition current, String entityType) {
        List<FieldDiff> allDiffs = new ArrayList<>();
        VvoDefinition cursor = current;
        UUID firstAncestorId = current.getId();
        String firstAncestorKey = current.getIssueKey();

        // Walk the clone chain backwards
        while (cursor.getCloneSourceId() != null) {
            VvoDefinition parent = vvoRepo.findById(cursor.getCloneSourceId()).orElse(null);
            if (parent == null) {
                break;
            }

            List<FieldDiff> stepDiffs = compareEntities(parent, cursor);
            // Tag each diff with the version step
            for (FieldDiff diff : stepDiffs) {
                diff.setFieldName("[v" + parent.getVvoVersion() + "->v" + cursor.getVvoVersion() + "] "
                        + diff.getFieldName());
            }
            allDiffs.addAll(stepDiffs);

            firstAncestorId = parent.getId();
            firstAncestorKey = parent.getIssueKey();
            cursor = parent;
        }

        String html = generateHtmlReport(allDiffs, entityType, firstAncestorKey, current.getIssueKey());

        return DiffHistoryResponse.builder()
                .entityId(current.getId())
                .comparedWithId(firstAncestorId)
                .entityType(entityType)
                .mode("evolution")
                .diffs(allDiffs)
                .htmlReport(html)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Build an evolution report for a TechEvent by comparing with its supplier sync source.
     */
    private DiffHistoryResponse buildTechEventEvolutionReport(TechEvent current) {
        List<FieldDiff> diffs = new ArrayList<>();
        UUID comparedWithId = current.getId();

        // TechEvents use supplierSyncIssueId for tracking related entities
        if (current.getSupplierSyncIssueId() != null) {
            TechEvent source = techEventRepo.findById(current.getSupplierSyncIssueId()).orElse(null);
            if (source != null) {
                diffs = compareEntities(source, current);
                comparedWithId = source.getId();
            }
        }

        String html = generateHtmlReport(diffs, "TechEvent",
                comparedWithId.toString(), current.getIssueKey());

        return DiffHistoryResponse.builder()
                .entityId(current.getId())
                .comparedWithId(comparedWithId)
                .entityType("TechEvent")
                .mode("evolution")
                .diffs(diffs)
                .htmlReport(html)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Compare two entity instances field-by-field using reflection.
     * Returns a list of FieldDiff for all fields that differ.
     */
    private <T> List<FieldDiff> compareEntities(T baseline, T current) {
        List<FieldDiff> diffs = new ArrayList<>();
        Class<?> clazz = baseline.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (SKIP_FIELDS.contains(field.getName())) {
                continue;
            }

            field.setAccessible(true);

            try {
                Object oldVal = field.get(baseline);
                Object newVal = field.get(current);

                String oldStr = formatValue(oldVal);
                String newStr = formatValue(newVal);

                if (Objects.equals(oldStr, newStr)) {
                    continue;
                }

                String changeType;
                if (isBlank(oldStr) && !isBlank(newStr)) {
                    changeType = "ADDED";
                } else if (!isBlank(oldStr) && isBlank(newStr)) {
                    changeType = "REMOVED";
                } else {
                    changeType = "MODIFIED";
                }

                diffs.add(FieldDiff.builder()
                        .fieldName(field.getName())
                        .oldValue(oldStr)
                        .newValue(newStr)
                        .changeType(changeType)
                        .build());
            } catch (IllegalAccessException e) {
                log.debug("Could not access field {} for comparison", field.getName());
            }
        }

        return diffs;
    }

    /**
     * Generate an HTML report with color-coded changes.
     */
    private String generateHtmlReport(List<FieldDiff> diffs, String entityType,
                                       String baselineLabel, String currentLabel) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("h2 { color: #333; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 10px; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }\n");
        html.append(".added { background-color: #d4edda; }\n");
        html.append(".removed { background-color: #f8d7da; }\n");
        html.append(".modified { background-color: #fff3cd; }\n");
        html.append(".unchanged { background-color: #ffffff; }\n");
        html.append("</style></head><body>\n");

        html.append("<h2>").append(entityType).append(" Diff Report</h2>\n");
        html.append("<p><strong>Baseline:</strong> ").append(escapeHtml(baselineLabel))
                .append(" &rarr; <strong>Current:</strong> ").append(escapeHtml(currentLabel)).append("</p>\n");

        if (diffs.isEmpty()) {
            html.append("<p>No differences found.</p>\n");
        } else {
            html.append("<table>\n");
            html.append("<tr><th>Field</th><th>Old Value</th><th>New Value</th><th>Change</th></tr>\n");

            for (FieldDiff diff : diffs) {
                String cssClass;
                switch (diff.getChangeType()) {
                    case "ADDED":
                        cssClass = "added";
                        break;
                    case "REMOVED":
                        cssClass = "removed";
                        break;
                    case "MODIFIED":
                        cssClass = "modified";
                        break;
                    default:
                        cssClass = "unchanged";
                }

                html.append("<tr class=\"").append(cssClass).append("\">");
                html.append("<td>").append(escapeHtml(diff.getFieldName())).append("</td>");
                html.append("<td>").append(escapeHtml(diff.getOldValue())).append("</td>");
                html.append("<td>").append(escapeHtml(diff.getNewValue())).append("</td>");
                html.append("<td>").append(diff.getChangeType()).append("</td>");
                html.append("</tr>\n");
            }

            html.append("</table>\n");
        }

        html.append("<p><em>Generated at: ").append(LocalDateTime.now()).append("</em></p>\n");
        html.append("</body></html>");

        return html.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Collection<?> coll) {
            if (coll.isEmpty()) {
                return "";
            }
            return coll.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
        return value.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
