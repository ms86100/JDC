package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.IssueTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IssueTypeRepository extends JpaRepository<IssueTypeEntity, String> {
    Optional<IssueTypeEntity> findByName(String name);
}