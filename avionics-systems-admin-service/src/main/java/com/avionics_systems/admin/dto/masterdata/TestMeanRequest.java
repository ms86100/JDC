package com.avionics_systems.admin.dto.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TestMeanRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    private String description;

    @NotBlank(message = "Program ID is required")
    private String programId;

    private String category;

    private Integer displayOrder;
}
