package com.jira.test.repository;

import com.jira.test.entity.UserProjectAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProjectAccessRepository extends JpaRepository<UserProjectAccess, UUID> {

    List<UserProjectAccess> findByUserId(UUID userId);

    List<UserProjectAccess> findByProjectId(UUID projectId);

    Optional<UserProjectAccess> findByUserIdAndProjectId(UUID userId, UUID projectId);

    boolean existsByUserIdAndProjectId(UUID userId, UUID projectId);

    boolean existsByUserIdAndProjectIdAndRole(UUID userId, UUID projectId, String role);

    void deleteByUserIdAndProjectId(UUID userId, UUID projectId);
}