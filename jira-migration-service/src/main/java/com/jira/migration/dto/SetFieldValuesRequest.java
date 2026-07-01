package com.jira.migration.dto;

import lombok.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetFieldValuesRequest {
    private UUID issueId;
    private Map<String, Object> values;
}