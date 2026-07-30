package com.avionics_systems.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemSettingsEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false)
    private String settingKey;
    @Column(columnDefinition = "TEXT")
    private String settingValue;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private String category;
    private String dataType;
    private Boolean isSensitive;
    private Boolean isSystem;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
