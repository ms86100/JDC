package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByIssueIdAndFieldId(UUID issueId, UUID fieldId);

    List<Label> findByFieldId(UUID fieldId);

    List<Label> findByValue(String value);

    void deleteByIssueIdAndFieldId(UUID issueId, UUID fieldId);

    void deleteByIssueIdAndFieldIdAndValue(UUID issueId, UUID fieldId, String value);

    boolean existsByIssueIdAndFieldIdAndValue(UUID issueId, UUID fieldId, String value);

    long countByFieldId(UUID fieldId);
}