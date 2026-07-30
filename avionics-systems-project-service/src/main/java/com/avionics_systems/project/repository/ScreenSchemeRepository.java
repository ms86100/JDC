package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ScreenScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenSchemeRepository extends JpaRepository<ScreenScheme, UUID> {

    Optional<ScreenScheme> findByIsDefaultTrue();

    Optional<ScreenScheme> findByName(String name);

    List<ScreenScheme> findByNameContainingIgnoreCase(String name);
}