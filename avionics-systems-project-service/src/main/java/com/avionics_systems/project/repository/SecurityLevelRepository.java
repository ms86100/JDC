package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.SecurityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityLevelRepository extends JpaRepository<SecurityLevel, UUID> {
    List<SecurityLevel> findBySchemeId(UUID schemeId);
    List<SecurityLevel> findBySchemeIdOrderBySequence(UUID schemeId);
}