package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.TestFolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestFolderRepository extends JpaRepository<TestFolder, UUID> {

    List<TestFolder> findByProjectIdOrderBySortOrderAsc(UUID projectId);

    List<TestFolder> findByProjectIdAndParentIdIsNullOrderBySortOrderAsc(UUID projectId);

    List<TestFolder> findByParentIdOrderBySortOrderAsc(UUID parentId);

    Optional<TestFolder> findByProjectIdAndName(UUID projectId, String name);

    @Query("SELECT f FROM TestFolder f WHERE f.projectId = :projectId AND f.path LIKE :pathPrefix%")
    List<TestFolder> findByPathPrefix(@Param("projectId") UUID projectId, @Param("pathPrefix") String pathPrefix);

    @Query("SELECT f FROM TestFolder f WHERE f.projectId = :projectId AND f.folderType = :type ORDER BY f.sortOrder")
    List<TestFolder> findByProjectIdAndType(@Param("projectId") UUID projectId, @Param("type") String type);

    @Query("SELECT f FROM TestFolder f WHERE f.projectId = :projectId AND f.isStarred = true")
    List<TestFolder> findStarredFolders(@Param("projectId") UUID projectId);

    @Query("SELECT f FROM TestFolder f WHERE f.projectId = :projectId AND f.depth <= :maxDepth ORDER BY f.path")
    List<TestFolder> findByProjectIdWithMaxDepth(@Param("projectId") UUID projectId, @Param("maxDepth") int maxDepth);

    @Query("SELECT COUNT(t) FROM TestFolder t WHERE t.parentId = :parentId")
    long countChildren(@Param("parentId") UUID parentId);

    @Query("SELECT f FROM TestFolder f WHERE f.projectId = :projectId AND LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<TestFolder> searchByName(@Param("projectId") UUID projectId, @Param("query") String query, Pageable pageable);

    @Query("SELECT f FROM TestFolder f WHERE f.projectId = :projectId ORDER BY f.updatedAt DESC")
    List<TestFolder> findRecentlyModified(@Param("projectId") UUID projectId, Pageable pageable);
}