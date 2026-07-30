package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenConfigurationResponse {
    private String screenType;
    private List<FieldDefinitionResponse> headerFields;
    private List<FieldDefinitionResponse> leftPrimaryFields;
    private List<FieldDefinitionResponse> leftDescriptionFields;
    private List<FieldDefinitionResponse> leftActivityFields;
    private List<FieldDefinitionResponse> sidebarPeopleFields;
    private List<FieldDefinitionResponse> sidebarDetailsFields;
    private List<FieldDefinitionResponse> sidebarTimeFields;
    private List<FieldDefinitionResponse> sidebarAgileFields;
    private List<FieldDefinitionResponse> sidebarDatesFields;
    private List<FieldDefinitionResponse> sidebarVersionsFields;
    private List<FieldDefinitionResponse> customFields;
}