package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    long countByStatus(UserEntity.UserStatus status);
    org.springframework.data.domain.Page<UserEntity> findByUsernameContainingIgnoreCase(String search, org.springframework.data.domain.Pageable pageable);
}