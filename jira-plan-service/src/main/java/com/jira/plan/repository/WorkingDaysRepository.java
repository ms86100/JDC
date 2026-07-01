package com.jira.plan.repository;

import com.jira.plan.entity.WorkingDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkingDaysRepository extends JpaRepository<WorkingDays, UUID> {

    Optional<WorkingDays> findByIsDefaultTrue();

    List<WorkingDays> findByNameContainingIgnoreCase(String name);

    boolean existsByIsDefaultTrue();
}