package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.FieldConfigurationScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldConfigurationSchemeRepository extends JpaRepository<FieldConfigurationScheme, UUID> {

    Optional<FieldConfigurationScheme> findByIsDefaultTrue();
}
