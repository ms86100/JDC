package com.jira.test.repository;

import com.jira.test.entity.ScreenField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScreenFieldRepository extends JpaRepository<ScreenField, UUID> {

    List<ScreenField> findByScreenIdOrderByPositionAsc(UUID screenId);

    Optional<ScreenField> findByScreenIdAndFieldId(UUID screenId, UUID fieldId);

    boolean existsByScreenIdAndFieldId(UUID screenId, UUID fieldId);

    @Modifying
    @Query("UPDATE ScreenField sf SET sf.position = sf.position + 1 WHERE sf.screenId = :screenId AND sf.position >= :position")
    void incrementPositionsFrom(UUID screenId, int position);

    @Modifying
    @Query("UPDATE ScreenField sf SET sf.position = sf.position - 1 WHERE sf.screenId = :screenId AND sf.position > :position")
    void decrementPositionsAbove(UUID screenId, int position);

    void deleteByScreenIdAndFieldId(UUID screenId, UUID fieldId);

    @Query("SELECT MAX(sf.position) FROM ScreenField sf WHERE sf.screenId = :screenId")
    Optional<Integer> findMaxPositionByScreenId(@Param("screenId") UUID screenId);

    @Modifying
    @Query("UPDATE ScreenField sf SET sf.position = sf.position - 1 WHERE sf.screenId = :screenId AND sf.position > :fromPos AND sf.position <= :toPos")
    void decrementPositionsBetween(@Param("screenId") UUID screenId, @Param("fromPos") int fromPos, @Param("toPos") int toPos);

    @Modifying
    @Query("UPDATE ScreenField sf SET sf.position = sf.position + 1 WHERE sf.screenId = :screenId AND sf.position >= :fromPos AND sf.position < :toPos")
    void incrementPositionsBetween(@Param("screenId") UUID screenId, @Param("fromPos") int fromPos, @Param("toPos") int toPos);
}