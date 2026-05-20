package com.jira.admin.repository;

import com.jira.admin.entity.StatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<StatusEntity, String> {

    Optional<StatusEntity> findByName(String name);

    List<StatusEntity> findByIsArchivedFalseOrderBySequenceAsc();

    List<StatusEntity> findByIsActiveTrueOrderBySequenceAsc();

    List<StatusEntity> findByStatusCategoryOrderBySequenceAsc(String statusCategory);

    Optional<StatusEntity> findByIsDefaultTrue();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);
}