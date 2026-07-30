package com.avionics_systems.auth.repository;

import com.avionics_systems.auth.entity.UserGroupMembership;
import com.avionics_systems.auth.entity.UserGroupMembershipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembership, UserGroupMembershipId> {
    List<UserGroupMembership> findByUserId(UUID userId);
    List<UserGroupMembership> findByGroupId(UUID groupId);
    boolean existsByUserIdAndGroupId(UUID userId, UUID groupId);
    void deleteByUserIdAndGroupId(UUID userId, UUID groupId);
}