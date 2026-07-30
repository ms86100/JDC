package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.FieldConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FieldConfigurationRepository extends JpaRepository<FieldConfigurationEntity, String> {

    Optional<FieldConfigurationEntity> findByName(String name);

    Optional<FieldConfigurationEntity> findByIsDefaultTrue();
}