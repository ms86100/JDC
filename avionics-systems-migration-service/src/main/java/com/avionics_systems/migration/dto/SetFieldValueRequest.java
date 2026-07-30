package com.avionics_systems.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetFieldValueRequest {
    private String fieldKey;
    private Object value;
    private String source;
}