package com.jira.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserGroupMembershipRepository extends JpaRepository<Object, UUID> {

    @Query("SELECT g.id FROM UserGroupMembershipEntity g WHERE g.userId = :userId")
    List<UUID> findGroupIdsByUserId(@Param("userId") String userId);
}