package com.avionics_systems.component.repository;

import com.avionics_systems.component.entity.IssueComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface IssueComponentRepository extends JpaRepository<IssueComponent, UUID> {

    List<IssueComponent> findByIssueId(UUID issueId);

    List<IssueComponent> findByComponentId(UUID componentId);

    boolean existsByIssueIdAndComponentId(UUID issueId, UUID componentId);

    @Modifying
    @Query("DELETE FROM IssueComponent ic WHERE ic.issueId = :issueId")
    void deleteByIssueId(@Param("issueId") UUID issueId);

    @Modifying
    @Query("DELETE FROM IssueComponent ic WHERE ic.componentId = :componentId")
    void deleteByComponentId(@Param("componentId") UUID componentId);

    @Query("SELECT COUNT(ic) FROM IssueComponent ic WHERE ic.componentId = :componentId")
    long countByComponentId(@Param("componentId") UUID componentId);
}