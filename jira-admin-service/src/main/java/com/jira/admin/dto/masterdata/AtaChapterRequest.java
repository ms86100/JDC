package com.jira.admin.dto.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AtaChapterRequest {

    @NotBlank(message = "Chapter number is required")
    @Size(max = 10)
    private String chapterNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Program ID is required")
    private String programId;

    private Integer displayOrder;
}
