package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueStatusRepository extends JpaRepository<IssueStatus, UUID> {

    Optional<IssueStatus> findByName(String name);

    Optional<IssueStatus> findFirstByCategoryOrderBySequenceAsc(String category);

    @Query("SELECT s FROM IssueStatus s ORDER BY s.sequence ASC")
    List<IssueStatus> findCatalogStatuses();
}