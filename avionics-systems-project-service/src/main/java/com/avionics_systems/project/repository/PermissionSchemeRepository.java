package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.PermissionScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionSchemeRepository extends JpaRepository<PermissionScheme, UUID> {

    Optional<PermissionScheme> findByIsDefaultTrue();

    Optional<PermissionScheme> findByName(String name);

    List<PermissionScheme> findByNameContainingIgnoreCase(String name);
}