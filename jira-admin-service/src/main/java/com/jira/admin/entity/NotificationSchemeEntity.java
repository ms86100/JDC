package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "notification_schemes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSchemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String events;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}