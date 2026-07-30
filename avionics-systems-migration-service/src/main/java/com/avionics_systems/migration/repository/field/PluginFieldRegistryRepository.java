package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.PluginFieldRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PluginFieldRegistryRepository extends JpaRepository<PluginFieldRegistry, UUID> {

    List<PluginFieldRegistry> findByPluginKey(String pluginKey);

    Optional<PluginFieldRegistry> findByPluginKeyAndFieldKey(String pluginKey, String fieldKey);

    List<PluginFieldRegistry> findByLegacyFieldKey(String legacyFieldKey);

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE pfr.enabled = true AND pfr.deployed = true")
    List<PluginFieldRegistry> findAllActive();

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE pfr.pluginKey = :pluginKey AND pfr.enabled = true")
    List<PluginFieldRegistry> findActiveByPluginKey(String pluginKey);

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE pfr.searchable = true AND pfr.enabled = true")
    List<PluginFieldRegistry> findSearchable();

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE pfr.navigable = true AND pfr.enabled = true")
    List<PluginFieldRegistry> findNavigable();

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE pfr.fieldDefinitionId = :fieldDefId")
    List<PluginFieldRegistry> findByFieldDefinitionId(UUID fieldDefId);

    @Query("SELECT DISTINCT pfr.pluginKey FROM PluginFieldRegistry pfr")
    List<String> findDistinctPluginKeys();

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE LOWER(pfr.pluginName) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<PluginFieldRegistry> searchByPluginName(String query, Pageable pageable);

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE pfr.enabled = false")
    List<PluginFieldRegistry> findAllDisabled();

    @Query("SELECT pfr FROM PluginFieldRegistry pfr WHERE pfr.deployed = false")
    List<PluginFieldRegistry> findAllUndeployed();
}