package com.jira.plan.repository;

import com.jira.plan.entity.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    List<Sprint> findByBoardConfigIdOrderBySequenceAsc(UUID boardId);

    Optional<Sprint> findByBoardConfigIdAndState(UUID boardId, String state);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.state = :state ORDER BY s.sequence ASC")
    List<Sprint> findByBoardConfigIdAndStateOrderBySequence(@Param("boardId") UUID boardId, @Param("state") String state);

    /**
     * Get max sequence with pessimistic lock to prevent race conditions.
     * Use this method when creating new sprints to avoid duplicate sequences.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT MAX(s.sequence) FROM Sprint s WHERE s.boardConfig.id = :boardId")
    Optional<Integer> getMaxSequenceWithLock(@Param("boardId") UUID boardId);

    /**
     * @deprecated Use getMaxSequenceWithLock() for thread-safe sequence generation.
     */
    @Deprecated
    @Query("SELECT MAX(s.sequence) FROM Sprint s WHERE s.boardConfig.id = :boardId")
    Integer getMaxSequence(@Param("boardId") UUID boardId);

    List<Sprint> findByState(String state);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.state IN ('ACTIVE', 'CLOSED') ORDER BY s.startDate DESC")
    List<Sprint> findRecentSprints(@Param("boardId") UUID boardId);

    @Query("SELECT AVG(s.velocity) FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.velocity > 0")
    Double getAverageVelocity(@Param("boardId") UUID boardId);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId ORDER BY s.sequence ASC")
    Page<Sprint> findByBoardConfigIdPaginated(@Param("boardId") UUID boardId, Pageable pageable);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.state = :state ORDER BY s.sequence ASC")
    Page<Sprint> findByBoardConfigIdAndStatePaginated(@Param("boardId") UUID boardId, @Param("state") String state, Pageable pageable);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.state IN :states ORDER BY s.sequence ASC")
    Page<Sprint> findByBoardConfigIdAndStateInPaginated(@Param("boardId") UUID boardId, @Param("states") List<String> states, Pageable pageable);

    @Query("SELECT s FROM Sprint s WHERE s.boardConfig.id = :boardId AND s.state = :state ORDER BY s.sequence ASC")
    List<Sprint> findAllByBoardConfigIdAndState(@Param("boardId") UUID boardId, @Param("state") String state);
}