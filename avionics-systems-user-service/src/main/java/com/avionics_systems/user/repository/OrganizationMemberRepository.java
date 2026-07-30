package com.avionics_systems.user.repository;

import com.avionics_systems.user.entity.OrganizationMember;
import com.avionics_systems.user.entity.OrganizationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, OrganizationMemberId> {

    List<OrganizationMember> findByOrgId(UUID orgId);

    List<OrganizationMember> findByUserId(UUID userId);

    boolean existsByOrgIdAndUserId(UUID orgId, UUID userId);
}