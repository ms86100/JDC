package com.avionics_systems.migration.entity.field;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "field_screen_mappings", schema = "jira_migration")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldScreenMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id")
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_type", nullable = false, length = 32)
    private FieldScreenType screenType;

    @Column(name = "field_key", nullable = false, length = 255)
    private String fieldKey;

    @Column(name = "tab_name", length = 128)
    @Builder.Default
    private String tabName = "default";

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "required_on_screen")
    @Builder.Default
    private Boolean requiredOnScreen = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum FieldScreenType {
        CREATE, EDIT, VIEW, TRANSITION
    }
}
