package com.jira.admin.repository;

import com.jira.admin.entity.ApplicationLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationLinkRepository extends JpaRepository<ApplicationLinkEntity, String> {
    List<ApplicationLinkEntity> findAllByOrderByCreatedAtDesc();
    Optional<ApplicationLinkEntity> findByPrimaryTrue();
    long countByApplicationType(String applicationType);
}
