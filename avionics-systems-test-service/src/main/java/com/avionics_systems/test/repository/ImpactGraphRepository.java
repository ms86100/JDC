package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ImpactGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImpactGraphRepository extends JpaRepository<ImpactGraph, UUID> {

    List<ImpactGraph> findBySourceTypeAndSourceId(String sourceType, UUID sourceId);

    List<ImpactGraph> findByTargetTypeAndTargetId(String targetType, UUID targetId);

    List<ImpactGraph> findByProjectId(UUID projectId);

    @Query("SELECT g FROM ImpactGraph g WHERE g.sourceType = :sourceType AND g.sourceId = :sourceId AND g.cascadeDepth <= :maxDepth")
    List<ImpactGraph> findBySourceWithMaxDepth(@Param("sourceType") String sourceType,
                                               @Param("sourceId") UUID sourceId,
                                               @Param("maxDepth") Integer maxDepth);

    @Query("SELECT g FROM ImpactGraph g WHERE g.targetType = :targetType AND g.targetId = :targetId")
    List<ImpactGraph> findDependents(@Param("targetType") String targetType, @Param("targetId") UUID targetId);

    @Query("SELECT DISTINCT g.sourceId FROM ImpactGraph g WHERE g.targetType = 'TEST' AND g.targetId IN :testIds")
    List<UUID> findRelatedTestIds(@Param("testIds") List<UUID> testIds);

    void deleteBySourceTypeAndSourceId(String sourceType, UUID sourceId);

    @Query("SELECT COUNT(g) FROM ImpactGraph g WHERE g.projectId = :projectId")
    Long countByProject(@Param("projectId") UUID projectId);
}