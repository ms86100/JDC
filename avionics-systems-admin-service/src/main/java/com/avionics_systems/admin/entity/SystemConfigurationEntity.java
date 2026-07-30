package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "system_configuration", schema = "jira_admin")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "config_key", nullable = false, unique = true, length = 200)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "value_type", nullable = false, length = 20)
    @Builder.Default
    private String valueType = "STRING";

    @Column(nullable = false, length = 50)
    private String category;

    @Column(length = 500)
    private String description;

    @Column(name = "is_editable", nullable = false)
    @Builder.Default
    private Boolean isEditable = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
