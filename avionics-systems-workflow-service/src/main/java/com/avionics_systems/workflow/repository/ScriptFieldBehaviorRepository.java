package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ScriptFieldBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScriptFieldBehaviorRepository extends JpaRepository<ScriptFieldBehavior, UUID> {

    List<ScriptFieldBehavior> findByScreenContextAndIsEnabledTrueOrderByExecutionOrderAsc(String screenContext);

    List<ScriptFieldBehavior> findByScriptId(UUID scriptId);

    void deleteByScriptId(UUID scriptId);
}
