package com.avionics_systems.issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestEdge {
    private TestResponse node;
    private String cursor;
}