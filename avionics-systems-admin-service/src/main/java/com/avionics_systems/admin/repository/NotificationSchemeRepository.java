package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.NotificationSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSchemeRepository extends JpaRepository<NotificationSchemeEntity, String> {
    Optional<NotificationSchemeEntity> findByName(String name);
    Optional<NotificationSchemeEntity> findByIsDefaultTrue();
}