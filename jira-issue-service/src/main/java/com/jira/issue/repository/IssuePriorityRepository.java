package com.jira.issue.repository;

import com.jira.issue.entity.IssuePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssuePriorityRepository extends JpaRepository<IssuePriority, UUID> {

    Optional<IssuePriority> findByName(String name);

    Optional<IssuePriority> findFirstByOrderBySequenceAsc();
}