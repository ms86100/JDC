package com.avionics_systems.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StatusDto {
    private String id;
    private String name;
    private String description;
    private String category;
    private String color;
    private String icon;
    private Boolean isActive;
    private Integer sequence;
}