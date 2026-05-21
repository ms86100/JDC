package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_folder_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestFolderTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    @Builder.Default
    private String category = "GENERAL";

    @Column(name = "folder_type", length = 50)
    @Builder.Default
    private String folderType = "FOLDER";

    @Column(columnDefinition = "TEXT")
    private String icon;

    @Column(columnDefinition = "TEXT")
    private String color;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "sub_folder_template", columnDefinition = "TEXT")
    private String subFolderTemplate;

    @Column(name = "default_test_fields", columnDefinition = "TEXT")
    private String defaultTestFields;

    @Column(name = "is_system_template")
    @Builder.Default
    private Boolean isSystemTemplate = false;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}