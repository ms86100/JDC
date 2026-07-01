package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "priorities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    private Integer sequence;

    private String statusColor;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}