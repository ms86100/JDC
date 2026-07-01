package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_info")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String key;

    private String value;

    private String category;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}