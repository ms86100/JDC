package com.jira.plan.repository;

import com.jira.plan.entity.PlanTeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanTeamMemberRepository extends JpaRepository<PlanTeamMember, UUID> {

    List<PlanTeamMember> findByTeamId(UUID teamId);

    Optional<PlanTeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}