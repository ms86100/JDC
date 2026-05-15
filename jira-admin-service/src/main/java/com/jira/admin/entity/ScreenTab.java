package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "screen_tabs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenTab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tab_name")
    private String tabName;

    @Column(columnDefinition = "TEXT")
    private String fieldIds;
}