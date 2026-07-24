package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets", schema = "jira_admin", indexes = {
        @Index(name = "idx_assets_type", columnList = "asset_type_id"),
        @Index(name = "idx_assets_status", columnList = "status"),
        @Index(name = "idx_assets_location", columnList = "location"),
        @Index(name = "idx_assets_serial", columnList = "serial_number")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_type_id", nullable = false)
    private UUID assetTypeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "sub_status", length = 50)
    private String subStatus;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "JSONB")
    private String attributes;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
