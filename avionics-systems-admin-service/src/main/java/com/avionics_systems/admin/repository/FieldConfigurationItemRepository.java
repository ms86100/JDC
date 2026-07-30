package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.FieldConfigurationItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldConfigurationItemRepository extends JpaRepository<FieldConfigurationItemEntity, String> {

    List<FieldConfigurationItemEntity> findByFieldConfigurationId(String fieldConfigurationId);

    Optional<FieldConfigurationItemEntity> findByFieldConfigurationIdAndFieldKey(String fieldConfigurationId, String fieldKey);
}