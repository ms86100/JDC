package com.jira.plan.repository;

import com.jira.plan.entity.CumulativeFlowData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CumulativeFlowDataRepository extends JpaRepository<CumulativeFlowData, UUID> {
    List<CumulativeFlowData> findByBoardIdAndDataDateBetweenOrderByDataDateAsc(UUID boardId, LocalDate startDate, LocalDate endDate);
    List<CumulativeFlowData> findBySprintIdOrderByDataDateAsc(UUID sprintId);
}