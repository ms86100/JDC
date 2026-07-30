package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.TemplateSchemeDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateSchemeDefaultRepository extends JpaRepository<TemplateSchemeDefault, UUID> {

    Optional<TemplateSchemeDefault> findByTemplateId(UUID templateId);
}