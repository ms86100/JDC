package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.EpicProgressHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpicProgressHistoryRepository extends JpaRepository<EpicProgressHistory, Long> {

    List<EpicProgressHistory> findByEpicIdOrderByRecordDateAsc(String epicId);

    List<EpicProgressHistory> findByEpicIdOrderByRecordDateDesc(String epicId);

    Optional<EpicProgressHistory> findByEpicIdAndRecordDate(String epicId, LocalDate recordDate);

    boolean existsByEpicIdAndRecordDate(String epicId, LocalDate recordDate);
}