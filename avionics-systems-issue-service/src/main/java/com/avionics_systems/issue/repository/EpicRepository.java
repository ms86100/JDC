package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.Epic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpicRepository extends JpaRepository<Epic, String> {

    List<Epic> findByLeadId(String leadId);

    List<Epic> findByStatus(String status);

    Optional<Epic> findByLinkedIssueId(String linkedIssueId);

    @Query("SELECT e FROM Epic e WHERE e.leadId = :userId OR e.leadName = :userName")
    List<Epic> findByLeadIdOrLeadName(@Param("userId") String userId, @Param("userName") String userName);

    @Modifying
    @Query("UPDATE Epic e SET e.totalStoryPoints = :total, e.completedStoryPoints = :completed, " +
           "e.totalIssueCount = :totalIssues, e.completedIssueCount = :completedIssues, e.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE e.id = :epicId")
    void updateProgress(@Param("epicId") String epicId,
                        @Param("total") BigDecimal total,
                        @Param("completed") BigDecimal completed,
                        @Param("totalIssues") Integer totalIssues,
                        @Param("completedIssues") Integer completedIssues);
}