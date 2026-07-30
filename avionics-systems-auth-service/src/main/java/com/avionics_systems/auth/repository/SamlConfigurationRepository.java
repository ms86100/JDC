package com.avionics_systems.auth.repository;

import com.avionics_systems.auth.entity.SamlConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SamlConfigurationRepository extends JpaRepository<SamlConfiguration, UUID> {

    Optional<SamlConfiguration> findByRegistrationId(String registrationId);

    List<SamlConfiguration> findByEnabledTrue();

    boolean existsByRegistrationId(String registrationId);

    Optional<SamlConfiguration> findByEntityId(String entityId);

    List<SamlConfiguration> findByEnabled(boolean enabled);
}
