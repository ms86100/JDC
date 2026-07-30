package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.VelocityHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VelocityHistoryRepository extends JpaRepository<VelocityHistory, String> {

    List<VelocityHistory> findByBoardIdOrderByCreatedAtDesc(String boardId);

    @Query("SELECT AVG(vh.completedPoints) FROM VelocityHistory vh WHERE vh.boardId = :boardId")
    Double getAverageVelocity(@Param("boardId") String boardId);

    @Query("SELECT AVG(vh.completedPoints) FROM VelocityHistory vh WHERE vh.boardId = :boardId AND vh.createdAt >= :since")
    Double getAverageVelocitySince(@Param("boardId") String boardId, @Param("since") java.time.LocalDateTime since);

    Optional<VelocityHistory> findTopByBoardIdOrderByCreatedAtDesc(String boardId);

    @Query("SELECT COUNT(vh) FROM VelocityHistory vh WHERE vh.boardId = :boardId")
    Integer getSprintCount(@Param("boardId") String boardId);
}