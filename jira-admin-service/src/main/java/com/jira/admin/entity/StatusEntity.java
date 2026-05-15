package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "statuses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status_category")
    private String statusCategory;

    private Integer sequence;

    @Column(name = "status_color")
    private String statusColor;

    @Column(name = "icon_url")
    private String iconUrl;
}