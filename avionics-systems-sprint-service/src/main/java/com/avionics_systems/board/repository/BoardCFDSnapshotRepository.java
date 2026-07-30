package com.avionics_systems.board.repository;

import com.avionics_systems.board.entity.BoardCFDSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardCFDSnapshotRepository extends JpaRepository<BoardCFDSnapshot, UUID> {
    @Query("SELECT s FROM BoardCFDSnapshot s WHERE s.boardId = :boardId AND s.snapshotDate BETWEEN :start AND :end ORDER BY s.snapshotDate, s.columnName")
    List<BoardCFDSnapshot> findByBoardIdAndDateRange(@Param("boardId") UUID boardId,
                                                      @Param("start") LocalDate start,
                                                      @Param("end") LocalDate end);
}
