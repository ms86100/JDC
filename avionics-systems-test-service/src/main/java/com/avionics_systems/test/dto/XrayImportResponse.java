package com.avionics_systems.test.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class XrayImportResponse {
    private String testExecIssue;
    private String id;
    private String key;
    private String self;
}
