package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldProvisioningResponse {
    private List<FieldDefinitionResponse> provisionedFields;
    private List<FieldDefinitionResponse> existingFields;
    private List<String> failedFields;
    private Map<String, String> fieldKeyMapping;
    private int totalProvisioned;
    private int totalExisting;
    private int totalFailed;
}