package com.avionics_systems.plan.repository;

import com.avionics_systems.plan.entity.PlanTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanTeamMemberRepository extends JpaRepository<PlanTeamMember, UUID> {

    List<PlanTeamMember> findByTeamId(UUID teamId);

    @Query("SELECT m FROM PlanTeamMember m WHERE m.teamId IN :teamIds")
    List<PlanTeamMember> findByTeamIds(@Param("teamIds") List<UUID> teamIds);

    Optional<PlanTeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}