package com.avionics_systems.test.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VvoCloneResponse {

    private UUID id;
    private String issueKey;
    private Integer vvoVersion;
    private UUID cloneSourceId;
    private String status;
}
