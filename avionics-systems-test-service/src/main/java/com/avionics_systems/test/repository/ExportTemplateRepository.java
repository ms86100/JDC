package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ExportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExportTemplateRepository extends JpaRepository<ExportTemplate, UUID> {

    List<ExportTemplate> findBySourceTypeOrderByNameAsc(String sourceType);

    Optional<ExportTemplate> findByName(String name);

    List<ExportTemplate> findByIsSystemTrueOrderByNameAsc();

    boolean existsByName(String name);
}
