package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "master_board_column_templates", schema = "jira_admin")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MasterBoardColumnTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_type_id", nullable = false)
    private MasterBoardTypeEntity boardType;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "status_category", nullable = false, length = 30)
    private String statusCategory;

    @Column(length = 7)
    private String color;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_mappings", columnDefinition = "jsonb")
    @Builder.Default
    private String statusMappings = "[]";
}
