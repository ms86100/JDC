package com.jira.plan.repository;

import com.jira.plan.entity.PlanItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanItemRepository extends JpaRepository<PlanItem, UUID> {

    List<PlanItem> findByPlanIdOrderBySortOrderAsc(UUID planId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId ORDER BY pi.sortOrder ASC")
    List<PlanItem> findByPlanIdSorted(@Param("planId") UUID planId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.parentId IS NULL ORDER BY pi.sortOrder ASC")
    List<PlanItem> findRootItemsByPlanId(@Param("planId") UUID planId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.parentId = :parentId ORDER BY pi.sortOrder ASC")
    List<PlanItem> findByPlanIdAndParentId(@Param("planId") UUID planId, @Param("parentId") UUID parentId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.issueType = :issueType ORDER BY pi.sortOrder ASC")
    List<PlanItem> findByPlanIdAndIssueType(@Param("planId") UUID planId, @Param("issueType") String issueType);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.assigneeId = :assigneeId ORDER BY pi.sortOrder ASC")
    List<PlanItem> findByPlanIdAndAssigneeId(@Param("planId") UUID planId, @Param("assigneeId") UUID assigneeId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.plan.id = :planId AND pi.issueType = 'EPIC' ORDER BY pi.sortOrder ASC")
    List<PlanItem> findEpicsByPlanId(@Param("planId") UUID planId);

    Optional<PlanItem> findByPlanIdAndIssueId(UUID planId, UUID issueId);

    @Query("SELECT MAX(pi.sortOrder) FROM PlanItem pi WHERE pi.plan.id = :planId")
    Optional<String> findMaxSortOrderByPlanId(@Param("planId") UUID planId);

    @Query("SELECT COUNT(pi) FROM PlanItem pi WHERE pi.parentId = :parentId")
    long countByParentId(@Param("parentId") UUID parentId);

    boolean existsByPlanIdAndIssueId(UUID planId, UUID issueId);

    List<PlanItem> findByPlanIdOrderBySortOrder(UUID planId);

    List<PlanItem> findByPlanIdAndParentIdIsNull(UUID planId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.sourceType = :sourceType AND pi.sourceId = :sourceId")
    List<PlanItem> findBySourceInfo(@Param("sourceType") String sourceType, @Param("sourceId") String sourceId);

    @Query("SELECT pi FROM PlanItem pi WHERE pi.planId = :planId AND pi.isActive = true ORDER BY pi.sortOrder ASC")
    List<PlanItem> findByPlanIdAndIsActiveTrue(@Param("planId") UUID planId);

    List<PlanItem> findByParentId(UUID parentId);
}