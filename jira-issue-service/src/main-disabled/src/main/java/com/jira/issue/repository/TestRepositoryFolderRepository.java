package com.jira.issue.repository;

import com.jira.issue.entity.TestRepositoryFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestRepositoryFolderRepository extends JpaRepository<TestRepositoryFolder, UUID> {

    List<TestRepositoryFolder> findByProjectIdOrderBySortOrderAsc(UUID projectId);

    List<TestRepositoryFolder> findByParentFolderIdOrderBySortOrderAsc(UUID parentFolderId);

    Optional<TestRepositoryFolder> findByProjectIdAndName(UUID projectId, String name);

    @Query("SELECT f FROM TestRepositoryFolder f WHERE f.projectId = :projectId AND f.parentFolderId IS NULL ORDER BY f.sortOrder")
    List<TestRepositoryFolder> findRootFoldersByProject(@Param("projectId") UUID projectId);

    @Query("SELECT f FROM TestRepositoryFolder f WHERE f.isSmartFolder = true AND f.projectId = :projectId")
    List<TestRepositoryFolder> findSmartFoldersByProject(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(t) FROM TestRepositoryFolder t WHERE t.parentFolderId = :parentId")
    Long countChildren(@Param("parentId") UUID parentId);
}