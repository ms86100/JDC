package com.avionics_systems.test.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGenerationResponse {

    private String fileName;
    private String contentType;
    private byte[] content;
    private LocalDateTime generatedAt;
}
