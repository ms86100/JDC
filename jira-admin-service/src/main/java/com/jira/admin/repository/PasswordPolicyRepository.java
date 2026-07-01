package com.jira.admin.repository;

import com.jira.admin.entity.PasswordPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicyEntity, String> {
    Optional<PasswordPolicyEntity> findByName(String name);
}