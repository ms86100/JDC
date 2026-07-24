package com.jira.admin.repository;

import com.jira.admin.entity.UserGroupMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGroupMembershipRepository extends JpaRepository<UserGroupMembershipEntity, String> {

    List<UserGroupMembershipEntity> findByUserId(String userId);

    List<UserGroupMembershipEntity> findByGroupId(String groupId);

    boolean existsByUserIdAndGroupId(String userId, String groupId);

    @Query("SELECT ugm.groupId FROM UserGroupMembershipEntity ugm WHERE ugm.userId = :userId")
    List<String> findGroupIdsByUserId(@Param("userId") String userId);

    @Query("SELECT ugm.userId FROM UserGroupMembershipEntity ugm WHERE ugm.groupId = :groupId")
    List<String> findUserIdsByGroupId(@Param("groupId") String groupId);

    void deleteByUserIdAndGroupId(String userId, String groupId);
}