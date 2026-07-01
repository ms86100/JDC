package com.jira.issue.repository;

import com.jira.issue.entity.TestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestSetRepository extends JpaRepository<TestSet, UUID> {

    List<TestSet> findByProjectIdAndArchivedFalseOrderByNameAsc(UUID projectId);

    Optional<TestSet> findByProjectIdAndName(UUID projectId, String name);

    List<TestSet> findByFolderIdAndArchivedFalse(UUID folderId);

    @Query("SELECT ts FROM TestSet ts WHERE ts.projectId = :projectId AND ts.status = :status AND ts.archived = false")
    List<TestSet> findByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") String status);

    @Query("SELECT ts FROM TestSet ts WHERE ts.projectId = :projectId AND ts.testType = :testType AND ts.archived = false")
    List<TestSet> findByProjectIdAndTestType(@Param("projectId") UUID projectId, @Param("testType") String testType);

    @Query("SELECT COUNT(t) FROM TestSet t WHERE t.folderId = :folderId AND t.archived = false")
    Long countByFolderId(@Param("folderId") UUID folderId);

    @Query("SELECT ts FROM TestSet ts WHERE ts.projectId = :projectId AND ts.labels @> :label")
    List<TestSet> findByProjectIdAndLabel(@Param("projectId") UUID projectId, @Param("label") String label);
}