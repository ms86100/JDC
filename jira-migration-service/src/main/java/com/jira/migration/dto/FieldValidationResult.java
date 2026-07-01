package com.jira.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidationResult {
    private boolean valid;
    private String fieldKey;
    private Object value;
    private String validationStatus;
    private String message;
    private List<String> warnings;
}