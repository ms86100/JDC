package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.CumulativeFlowSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CumulativeFlowSnapshotRepository extends JpaRepository<CumulativeFlowSnapshot, Long> {

    @Query("SELECT c FROM CumulativeFlowSnapshot c WHERE c.boardId = :boardId AND c.snapshotDate BETWEEN :from AND :to ORDER BY c.snapshotDate ASC, c.columnName ASC")
    List<CumulativeFlowSnapshot> findByBoardIdAndDateRange(@Param("boardId") UUID boardId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
