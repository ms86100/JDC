package com.jira.admin.repository;

import com.jira.admin.entity.AtaChapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtaChapterRepository extends JpaRepository<AtaChapterEntity, String> {

    List<AtaChapterEntity> findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(String programId);

    boolean existsByProgramIdAndChapterNumber(String programId, String chapterNumber);
}
