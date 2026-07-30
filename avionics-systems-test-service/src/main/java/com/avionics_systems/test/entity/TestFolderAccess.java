package com.avionics_systems.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_folder_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestFolderAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "folder_id", nullable = false)
    private UUID folderId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "group_id")
    private UUID groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false)
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.READ;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AccessLevel {
        READ,
        WRITE,
        ADMIN
    }
}