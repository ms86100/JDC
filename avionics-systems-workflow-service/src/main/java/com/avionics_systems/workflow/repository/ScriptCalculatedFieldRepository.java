package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ScriptCalculatedField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScriptCalculatedFieldRepository extends JpaRepository<ScriptCalculatedField, UUID> {

    Optional<ScriptCalculatedField> findByCustomFieldIdAndIsEnabledTrue(UUID customFieldId);

    List<ScriptCalculatedField> findByScriptId(UUID scriptId);

    void deleteByScriptId(UUID scriptId);
}
