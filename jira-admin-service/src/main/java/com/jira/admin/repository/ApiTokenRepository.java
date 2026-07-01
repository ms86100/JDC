package com.jira.admin.repository;

import com.jira.admin.entity.ApiTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiTokenEntity, String> {
    List<ApiTokenEntity> findByUserId(String userId);
    Optional<ApiTokenEntity> findByTokenHash(String tokenHash);
}