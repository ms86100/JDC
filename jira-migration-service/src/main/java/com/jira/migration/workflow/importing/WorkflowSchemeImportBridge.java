package com.jira.migration.workflow.importing;

import com.jira.migration.service.clients.WorkflowSchemeServiceClient;
import com.jira.migration.workflow.model.WorkflowSchemeModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowSchemeImportBridge {

    private final WorkflowSchemeServiceClient schemeServiceClient;

    public Map<String, Object> importScheme(WorkflowSchemeModel scheme, UUID defaultWorkflowId, boolean stubDownstream) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemeName", scheme.getName());
        result.put("mappingsRequested", scheme.getMappings().size());

        if (stubDownstream || scheme.getName() == null) {
            result.put("status", "STUB");
            result.put("targetSchemeId", "stub-scheme-" + UUID.randomUUID());
            result.put("message", "Scheme import recorded locally (stubDownstream or missing name)");
            return result;
        }

        try {
            Map<String, Object> created = schemeServiceClient.createScheme(
                    scheme.getName(),
                    scheme.getMeta().getOrDefault("jira.description", "DC scheme import"),
                    defaultWorkflowId);
            String schemeId = String.valueOf(created.getOrDefault("id", created.get("schemeId")));
            result.put("status", "CREATED");
            result.put("targetSchemeId", schemeId);
            result.put("schemeResponse", created);

            List<Map<String, Object>> mappingResults = new ArrayList<>();
            for (WorkflowSchemeModel.WorkflowSchemeMapping m : scheme.getMappings()) {
                mappingResults.add(Map.of(
                        "issueType", m.getIssueType(),
                        "workflow", m.getWorkflow(),
                        "status", "PENDING_ISSUE_TYPE_RESOLUTION",
                        "note", "Map issue type name to UUID in Migration Center advanced mapping"
                ));
            }
            result.put("mappingResults", mappingResults);
            result.put("projectAssociations", scheme.getProjectAssociations());
        } catch (Exception e) {
            log.warn("Scheme service import failed: {}", e.getMessage());
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
        }
        return result;
    }
}
