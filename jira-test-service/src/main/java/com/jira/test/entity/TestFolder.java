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
@Table(name = "test_folder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "folder_type", length = 50)
    @Builder.Default
    private String folderType = "FOLDER"; // FOLDER, SMART_FOLDER, TEST_SET_FOLDER

    @Column(columnDefinition = "TEXT")
    private String path; // Full path like /parent/child/grandchild

    @Column
    @Builder.Default
    private Integer depth = 0;

    @Column
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(columnDefinition = "TEXT")
    private String icon; // emoji or icon class

    @Column(columnDefinition = "TEXT")
    private String color; // hex color for UI

    @Column(columnDefinition = "TEXT")
    private String filterCriteria; // JSON for smart folders

    @Column
    @Builder.Default
    private Boolean isStarred = false;

    @Column
    @Builder.Default
    private Boolean isExpanded = true;

    @Column(columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private TestFolder parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TestFolder> children = new ArrayList<>();
}