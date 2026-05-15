package com.jira.project.repository;

import com.jira.project.entity.IssueTypeScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueTypeSchemeRepository extends JpaRepository<IssueTypeScheme, UUID> {

    Optional<IssueTypeScheme> findByIsDefaultTrue();

    List<IssueTypeScheme> findByNameContainingIgnoreCase(String name);
}