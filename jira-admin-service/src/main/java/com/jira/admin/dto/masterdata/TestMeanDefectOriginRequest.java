package com.jira.admin.dto.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TestMeanDefectOriginRequest {

    @NotBlank(message = "Category is required")
    @Size(max = 255)
    private String category;

    private String subItem;

    private String parentId;

    private Integer displayOrder;
}
