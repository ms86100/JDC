package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ScriptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScriptVersionRepository extends JpaRepository<ScriptVersion, UUID> {

    List<ScriptVersion> findByScriptIdOrderByVersionDesc(UUID scriptId);

    Optional<ScriptVersion> findByScriptIdAndVersion(UUID scriptId, Integer version);
}
