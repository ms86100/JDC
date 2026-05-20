package com.jira.plan.repository;

import com.jira.plan.entity.InitiativeEpic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InitiativeEpicRepository extends JpaRepository<InitiativeEpic, UUID> {

    List<InitiativeEpic> findByInitiativeIdOrderBySequenceAsc(UUID initiativeId);

    List<InitiativeEpic> findByEpicId(UUID epicId);

    Optional<InitiativeEpic> findByInitiativeIdAndEpicId(UUID initiativeId, UUID epicId);

    @Query("SELECT SUM(ie.totalStoryPoints) FROM InitiativeEpic ie WHERE ie.initiativeId = :initiativeId")
    Integer sumTotalStoryPointsByInitiativeId(@Param("initiativeId") UUID initiativeId);

    @Query("SELECT SUM(ie.completedStoryPoints) FROM InitiativeEpic ie WHERE ie.initiativeId = :initiativeId")
    Integer sumCompletedStoryPointsByInitiativeId(@Param("initiativeId") UUID initiativeId);

    @Modifying
    @Query("UPDATE InitiativeEpic ie SET ie.completedStoryPoints = :completedPoints, ie.progressPercentage = :progress WHERE ie.epicId = :epicId")
    void updateEpicProgress(@Param("epicId") UUID epicId, @Param("completedPoints") Integer completedPoints, @Param("progress") Double progress);

    boolean existsByInitiativeIdAndEpicId(UUID initiativeId, UUID epicId);
}