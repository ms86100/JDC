package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTransitionResponse {
    private UUID issueId;
    private UUID workflowId;
    private UUID currentStatusId;
    private List<AvailableTransitionItem> transitions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableTransitionItem {
        private UUID id;
        private String name;
        private String description;
        private UUID toStatusId;
        private String toStatusName;
        private UUID screenId;
        private boolean hasScreen;
        private List<TransitionScreenFieldDto> screenFields;
        /** Project permission required (from transition metadata), e.g. RESOLVE_ISSUES */
        private String requiredPermission;
    }
}
