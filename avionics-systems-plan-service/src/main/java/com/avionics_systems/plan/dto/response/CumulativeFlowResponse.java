package com.avionics_systems.plan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CumulativeFlowResponse {
    private UUID boardId;
    private UUID sprintId;
    private List<String> columns;
    private List<CfdDataPoint> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CfdDataPoint {
        private LocalDate date;
        private Map<String, Integer> columnCounts;
    }
}
