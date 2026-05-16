package com.jira.project.repository;

import com.jira.project.entity.SecurityLevelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityLevelMemberRepository extends JpaRepository<SecurityLevelMember, UUID> {
    List<SecurityLevelMember> findBySecurityLevelId(UUID securityLevelId);
    List<SecurityLevelMember> findByMemberId(UUID memberId);
    List<SecurityLevelMember> findByGroupName(String groupName);
    boolean existsBySecurityLevelIdAndMemberId(UUID securityLevelId, UUID memberId);
}