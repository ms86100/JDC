package com.avionics_systems.plan.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloseSprintRequest {
    private UUID moveIncompleteToSprintId;
}
