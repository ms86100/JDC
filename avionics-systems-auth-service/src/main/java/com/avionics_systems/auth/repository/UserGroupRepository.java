package com.avionics_systems.auth.repository;

import com.avionics_systems.auth.entity.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, UUID> {
    Optional<UserGroup> findByGroupName(String groupName);
    boolean existsByGroupName(String groupName);
    List<UserGroup> findByIsActiveTrue();
}