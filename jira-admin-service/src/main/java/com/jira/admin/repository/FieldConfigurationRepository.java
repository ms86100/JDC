package com.jira.admin.repository;

import com.jira.admin.entity.FieldConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FieldConfigurationRepository extends JpaRepository<FieldConfigurationEntity, String> {

    Optional<FieldConfigurationEntity> findByName(String name);

    Optional<FieldConfigurationEntity> findByIsDefaultTrue();
}