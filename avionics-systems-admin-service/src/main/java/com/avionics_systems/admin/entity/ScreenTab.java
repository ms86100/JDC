package com.avionics_systems.admin.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "screen_tabs")
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenTab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id")
    @JsonIgnore
    private ScreenEntity screen;

    @Column(name = "tab_name")
    private String tabName;

    @Column(name = "tab_order")
    private Integer tabOrder;

    @Column(columnDefinition = "TEXT")
    private String fieldIds;
}