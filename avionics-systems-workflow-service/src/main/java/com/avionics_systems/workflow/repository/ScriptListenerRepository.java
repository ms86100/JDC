package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ScriptListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScriptListenerRepository extends JpaRepository<ScriptListener, UUID> {

    List<ScriptListener> findByEventTypeAndIsEnabledTrueOrderByExecutionOrderAsc(String eventType);

    List<ScriptListener> findByScriptIdOrderByEventTypeAsc(UUID scriptId);

    List<ScriptListener> findByIsEnabledTrueOrderByExecutionOrderAsc();

    void deleteByScriptId(UUID scriptId);
}
