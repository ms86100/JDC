package com.jira.migration.dto.wizard;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWizardSessionRequest {
    private String step;
    private UUID targetProjectId;
    private List<Map<String, Object>> fieldMappings;
    private List<Map<String, Object>> userMappings;
    private Map<String, Object> importOptions;
    private Map<String, Object> sessionData;
}
