package com.jira.version.repository;

import com.jira.version.entity.ReleaseTrain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReleaseTrainRepository extends JpaRepository<ReleaseTrain, UUID> {

    List<ReleaseTrain> findByIsActiveTrue();

    @Query("SELECT rt FROM ReleaseTrain rt WHERE rt.isActive = true ORDER BY rt.startDate ASC")
    List<ReleaseTrain> findActiveOrderByStartDate();

    List<ReleaseTrain> findByNameContainingIgnoreCase(String name);
}