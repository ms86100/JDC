package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.IssueTypeScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueTypeSchemeRepository extends JpaRepository<IssueTypeScheme, UUID> {

    Optional<IssueTypeScheme> findByIsDefaultTrue();

    Optional<IssueTypeScheme> findByName(String name);

    List<IssueTypeScheme> findByNameContainingIgnoreCase(String name);
}