package com.jira.board.repository;

import com.jira.board.entity.FilterSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FilterSubscriptionRepository extends JpaRepository<FilterSubscription, UUID> {
    List<FilterSubscription> findByFilterId(UUID filterId);
    List<FilterSubscription> findByUserId(UUID userId);

    @Query("SELECT fs FROM FilterSubscription fs WHERE fs.isEnabled = true AND fs.nextRunAt <= :now")
    List<FilterSubscription> findDueSubscriptions(@Param("now") LocalDateTime now);
}
