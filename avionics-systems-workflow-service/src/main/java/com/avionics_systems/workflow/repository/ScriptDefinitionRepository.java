package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ScriptDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScriptDefinitionRepository extends JpaRepository<ScriptDefinition, UUID> {

    Optional<ScriptDefinition> findByScriptKey(String scriptKey);

    List<ScriptDefinition> findByScriptTypeOrderByNameAsc(String scriptType);

    List<ScriptDefinition> findByIsEnabledTrueOrderByNameAsc();

    List<ScriptDefinition> findByScriptTypeAndIsEnabledTrueOrderByNameAsc(String scriptType);

    boolean existsByScriptKey(String scriptKey);
}
