package com.jira.plan.repository;

import com.jira.plan.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    List<Sprint> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    Optional<Sprint> findByBoardConfigIdAndState(UUID boardId, String state);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.state = :state ORDER BY s.sequence ASC")
    List<Sprint> findByBoardConfigIdAndStateOrderBySequence(@Param("boardId") UUID boardId, @Param("state") String state);

    @Query("SELECT MAX(s.sequence) FROM Sprint s WHERE s.boardConfig.id = :boardId")
    Integer getMaxSequence(@Param("boardId") UUID boardId);

    List<Sprint> findByState(String state);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.state IN ('ACTIVE', 'CLOSED') ORDER BY s.startDate DESC")
    List<Sprint> findRecentSprints(@Param("boardId") UUID boardId);

    @Query("SELECT AVG(s.velocity) FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.velocity > 0")
    Double getAverageVelocity(@Param("boardId") UUID boardId);
}