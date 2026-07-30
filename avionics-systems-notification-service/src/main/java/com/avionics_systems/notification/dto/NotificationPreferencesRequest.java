package com.avionics_systems.notification.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesRequest {

    private Map<String, Boolean> preferences;
}