package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.NotificationScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationSchemeRepository extends JpaRepository<NotificationScheme, UUID> {

    Optional<NotificationScheme> findByIsDefaultTrue();

    Optional<NotificationScheme> findByName(String name);

    List<NotificationScheme> findByNameContainingIgnoreCase(String name);
}