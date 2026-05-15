package com.jira.sprint.service;

import com.jira.sprint.dto.FilterSubscriptionResponse;
import com.jira.sprint.dto.SubscriptionFrequency;
import com.jira.sprint.dto.SavedFilterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {

    // Mock data - in production, this would use repositories
    private final Map<UUID, SavedFilterResponse> filters = new HashMap<>();
    private final Map<UUID, FilterSubscriptionResponse> subscriptions = new HashMap<>();

    public List<SavedFilterResponse> getSavedFilters(UUID userId, String tab) {
        List<SavedFilterResponse> result = new ArrayList<>();

        // System filters (always available)
        if ("system".equals(tab)) {
            result.add(SavedFilterResponse.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                    .name("All Issues")
                    .jql("")
                    .owner("System")
                    .isShared(true)
                    .favorite(false)
                    .shareType("SYSTEM")
                    .isSystem(true)
                    .build());
            result.add(SavedFilterResponse.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                    .name("My Issues")
                    .jql("assignee = currentUser()")
                    .owner("System")
                    .isShared(true)
                    .favorite(false)
                    .shareType("SYSTEM")
                    .isSystem(true)
                    .build());
            result.add(SavedFilterResponse.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                    .name("Reported by Me")
                    .jql("reporter = currentUser()")
                    .owner("System")
                    .isShared(true)
                    .favorite(false)
                    .shareType("SYSTEM")
                    .isSystem(true)
                    .build());
            result.add(SavedFilterResponse.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
                    .name("Recently Updated")
                    .jql("ORDER BY updated DESC")
                    .owner("System")
                    .isShared(true)
                    .favorite(false)
                    .shareType("SYSTEM")
                    .isSystem(true)
                    .build());
            result.add(SavedFilterResponse.builder()
                    .id(UUID.fromString("00000000-0000-0000-0000-000000000005"))
                    .name("Open Issues")
                    .jql("status NOT IN (Done, Closed)")
                    .owner("System")
                    .isShared(true)
                    .favorite(false)
                    .shareType("SYSTEM")
                    .isSystem(true)
                    .build());
        } else {
            // User filters
            result.add(SavedFilterResponse.builder()
                    .id(UUID.randomUUID())
                    .name("Critical Bugs")
                    .jql("type = Bug AND priority IN (Highest, High)")
                    .owner(userId != null ? userId.toString() : "me")
                    .isShared(false)
                    .favorite(true)
                    .shareType("PRIVATE")
                    .isSystem(false)
                    .usageCount(15)
                    .lastUsed(LocalDateTime.now().minusHours(2))
                    .build());
            result.add(SavedFilterResponse.builder()
                    .id(UUID.randomUUID())
                    .name("Current Sprint")
                    .jql("sprint = \"Sprint 1\"")
                    .owner(userId != null ? userId.toString() : "me")
                    .isShared(true)
                    .favorite(false)
                    .shareType("PROJECT")
                    .isSystem(false)
                    .usageCount(8)
                    .lastUsed(LocalDateTime.now().minusDays(1))
                    .build());
            result.add(SavedFilterResponse.builder()
                    .id(UUID.randomUUID())
                    .name("Unassigned Tasks")
                    .jql("type = Task AND assignee is empty")
                    .owner(userId != null ? userId.toString() : "me")
                    .isShared(false)
                    .favorite(false)
                    .shareType("PRIVATE")
                    .isSystem(false)
                    .usageCount(5)
                    .lastUsed(LocalDateTime.now().minusDays(3))
                    .build());
        }

        return result;
    }

    public SavedFilterResponse createFilter(UUID userId, String name, String jql, boolean isShared) {
        SavedFilterResponse filter = SavedFilterResponse.builder()
                .id(UUID.randomUUID())
                .name(name)
                .jql(jql)
                .owner(userId != null ? userId.toString() : "me")
                .isShared(isShared)
                .favorite(false)
                .shareType(isShared ? "PROJECT" : "PRIVATE")
                .isSystem(false)
                .usageCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        filters.put(filter.getId(), filter);
        log.info("Created filter: {} for user: {}", name, userId);
        return filter;
    }

    public void deleteFilter(UUID filterId) {
        filters.remove(filterId);
        log.info("Deleted filter: {}", filterId);
    }

    public SavedFilterResponse toggleFavorite(UUID filterId) {
        SavedFilterResponse filter = filters.get(filterId);
        if (filter != null) {
            filter.setFavorite(!filter.getFavorite());
            filters.put(filterId, filter);
        }
        return filter;
    }

    public List<FilterSubscriptionResponse> getSubscriptions(UUID userId) {
        List<FilterSubscriptionResponse> result = new ArrayList<>();

        // Mock subscriptions
        result.add(FilterSubscriptionResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .filterName("Critical Bugs")
                .jqlQuery("type = Bug AND priority IN (Highest, High)")
                .frequency(SubscriptionFrequency.INSTANT)
                .isActive(true)
                .emailNotification(true)
                .lastNotified(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(7))
                .build());

        return result;
    }

    public FilterSubscriptionResponse createSubscription(UUID userId, String filterName, String jql,
                                                          com.jira.sprint.dto.SubscriptionFrequency frequency,
                                                          boolean emailNotification) {
        FilterSubscriptionResponse subscription = FilterSubscriptionResponse.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .filterName(filterName)
                .jqlQuery(jql)
                .frequency(frequency)
                .isActive(true)
                .emailNotification(emailNotification)
                .createdAt(LocalDateTime.now())
                .build();

        subscriptions.put(subscription.getId(), subscription);
        log.info("Created subscription: {} for user: {}", filterName, userId);
        return subscription;
    }

    public void deleteSubscription(UUID subscriptionId) {
        subscriptions.remove(subscriptionId);
        log.info("Deleted subscription: {}", subscriptionId);
    }

    public FilterSubscriptionResponse toggleSubscription(UUID subscriptionId) {
        FilterSubscriptionResponse subscription = subscriptions.get(subscriptionId);
        if (subscription != null) {
            subscription.setIsActive(!subscription.getIsActive());
            subscriptions.put(subscriptionId, subscription);
        }
        return subscription;
    }
}