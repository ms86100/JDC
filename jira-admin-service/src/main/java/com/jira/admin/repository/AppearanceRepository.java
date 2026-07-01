package com.jira.admin.repository;

import com.jira.admin.entity.AppearanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppearanceRepository extends JpaRepository<AppearanceEntity, String> {
}