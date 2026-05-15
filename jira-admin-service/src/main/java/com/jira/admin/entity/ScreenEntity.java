package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "screens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "screen_type")
    private String screenType;

    @Transient
    private java.util.List<ScreenTab> tabs;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}