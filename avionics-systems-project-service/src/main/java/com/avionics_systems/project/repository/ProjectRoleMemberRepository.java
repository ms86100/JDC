package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.ProjectRoleMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRoleMemberRepository extends JpaRepository<ProjectRoleMember, UUID> {
    List<ProjectRoleMember> findByProjectRoleId(UUID projectRoleId);
    List<ProjectRoleMember> findByProjectId(UUID projectId);
    List<ProjectRoleMember> findByMemberId(UUID memberId);
    List<ProjectRoleMember> findByGroupName(String groupName);
    boolean existsByProjectRoleIdAndMemberIdAndProjectId(UUID projectRoleId, UUID memberId, UUID projectId);
}