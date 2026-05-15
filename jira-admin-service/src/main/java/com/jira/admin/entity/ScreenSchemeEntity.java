package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "screen_schemes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSchemeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "create_screen_id")
    private String createScreenId;

    @Column(name = "edit_screen_id")
    private String editScreenId;

    @Column(name = "view_screen_id")
    private String viewScreenId;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}