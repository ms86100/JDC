package com.avionics_systems.test.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoorsImportResponse {

    private boolean success;
    private int updatedCount;
    private String errorMessage;
    private List<String> errors;
}
