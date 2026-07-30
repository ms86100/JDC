package com.avionics_systems.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationSchemeDto {
    private String id;
    private String name;
    private String description;
    private String defaultRecipients;
    private List<NotificationEventDto> events;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NotificationEventDto {
        private String eventType;
        private String notificationType;
        private String recipientType;
        private String groupId;
        private String userId;
        private String emailAddress;
        private String projectRoleId;
    }
}