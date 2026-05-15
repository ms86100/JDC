package com.jira.notification.repository;

import com.jira.notification.entity.NotificationPreference;
import com.jira.notification.entity.NotificationPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, NotificationPreferenceId> {

    List<NotificationPreference> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM jira_notification.notification_preferences n WHERE n.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}