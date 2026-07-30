package com.avionics_systems.migration.dto.wizard;

import lombok.*;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WizardExecuteImportRequest {
    private UUID targetProjectId;
    private Map<String, Object> options;
}
