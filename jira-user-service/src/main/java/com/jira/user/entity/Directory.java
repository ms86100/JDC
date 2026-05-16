package com.jira.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "directories", schema = "jira_admin")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "directory_name", nullable = false)
    private String directoryName;

    @Column(name = "directory_type", nullable = false)
    private String directoryType;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "order_index")
    private int orderIndex;
}