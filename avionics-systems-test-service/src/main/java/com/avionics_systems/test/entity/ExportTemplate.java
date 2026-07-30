package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "export_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_type", length = 20, nullable = false)
    @Builder.Default
    private String templateType = "CSV";

    @Column(name = "output_format", length = 20, nullable = false)
    @Builder.Default
    private String outputFormat = "CSV";

    @Column(columnDefinition = "JSONB")
    private String columns;

    @Column(name = "header_text", columnDefinition = "TEXT")
    private String headerText;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @Column(name = "group_by", length = 100)
    private String groupBy;

    @Column(name = "sort_by", length = 100)
    private String sortBy;

    @Column(name = "sort_direction", length = 4)
    @Builder.Default
    private String sortDirection = "ASC";

    @Column(name = "source_type", length = 30, nullable = false)
    private String sourceType;

    @Column(name = "filter_jql", columnDefinition = "TEXT")
    private String filterJql;

    @Column(name = "is_system")
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
