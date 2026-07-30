package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCreationResponse {

    private boolean success;
    private int vvosProcessed;
    private int executionsCreated;
    private String errorMessage;
    private List<String> logEntries;
    private List<String> errors;
}
