package com.jira.test.repository;

import com.jira.test.entity.SharedStepDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SharedStepDependencyRepository extends JpaRepository<SharedStepDependency, UUID> {

    List<SharedStepDependency> findByParentSharedStepId(UUID parentSharedStepId);

    List<SharedStepDependency> findByChildSharedStepId(UUID childSharedStepId);

    @Query("SELECT sd.childSharedStepId FROM SharedStepDependency sd WHERE sd.parentSharedStepId = :parentId")
    List<UUID> findChildIdsByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT sd.parentSharedStepId FROM SharedStepDependency sd WHERE sd.childSharedStepId = :childId")
    List<UUID> findParentIdsByChildId(@Param("childId") UUID childId);

    void deleteByParentSharedStepId(UUID parentSharedStepId);

    void deleteByChildSharedStepId(UUID childSharedStepId);
}