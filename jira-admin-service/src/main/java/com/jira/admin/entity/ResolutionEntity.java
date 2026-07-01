package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resolutions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    private Integer sequence;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}