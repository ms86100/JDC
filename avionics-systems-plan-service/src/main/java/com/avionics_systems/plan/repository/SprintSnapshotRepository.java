package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.SprintSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SprintSnapshotRepository extends JpaRepository<SprintSnapshot, String> {

    List<SprintSnapshot> findBySprintIdOrderByRecordDateAsc(String sprintId);

    List<SprintSnapshot> findBySprintIdOrderByRecordDateDesc(String sprintId);

    Optional<SprintSnapshot> findFirstBySprintIdAndSnapshotType(String sprintId, SprintSnapshot.SnapshotType snapshotType);

    @Query("SELECT ss FROM SprintSnapshot ss WHERE ss.sprintId = :sprintId AND ss.snapshotType = :type ORDER BY ss.recordDate DESC")
    List<SprintSnapshot> findBySprintIdAndType(@Param("sprintId") String sprintId, @Param("type") SprintSnapshot.SnapshotType type);

    boolean existsBySprintIdAndSnapshotType(String sprintId, SprintSnapshot.SnapshotType snapshotType);

    @Query("SELECT ss FROM SprintSnapshot ss WHERE ss.boardId = :boardId AND ss.snapshotType = 'COMMITMENT' ORDER BY ss.recordDate DESC")
    List<SprintSnapshot> findCommitmentSnapshotsByBoard(@Param("boardId") String boardId);
}