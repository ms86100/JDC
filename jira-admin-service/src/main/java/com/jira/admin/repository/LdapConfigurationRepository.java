package com.jira.admin.repository;

import com.jira.admin.entity.LdapConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LdapConfigurationRepository extends JpaRepository<LdapConfigurationEntity, String> {
    Optional<LdapConfigurationEntity> findByName(String name);
}