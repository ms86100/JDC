package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.ScriptSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScriptScheduleRepository extends JpaRepository<ScriptSchedule, UUID> {

    List<ScriptSchedule> findByIsEnabledTrueAndNextRunAtBefore(LocalDateTime now);

    Optional<ScriptSchedule> findByScriptId(UUID scriptId);

    void deleteByScriptId(UUID scriptId);
}
