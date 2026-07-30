package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ApiTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface ApiTokenRepository extends JpaRepository<ApiTokenEntity, String> {
    List<ApiTokenEntity> findByUserId(String userId);
    Optional<ApiTokenEntity> findByTokenHash(String tokenHash);
}