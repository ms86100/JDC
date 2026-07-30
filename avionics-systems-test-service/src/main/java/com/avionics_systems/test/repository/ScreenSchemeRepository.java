package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ScreenScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenSchemeRepository extends JpaRepository<ScreenScheme, UUID> {

    List<ScreenScheme> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<ScreenScheme> findByProjectIdAndName(UUID projectId, String name);

    Optional<ScreenScheme> findByProjectIdAndIsDefaultTrue(UUID projectId);

    boolean existsByProjectIdAndName(UUID projectId, String name);

    List<ScreenScheme> findByProjectIdAndNameContainingIgnoreCase(UUID projectId, String searchTerm);
}