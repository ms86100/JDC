package com.jira.sprint.service;

import com.jira.sprint.dto.FilterSubscriptionResponse;
import com.jira.sprint.dto.SavedFilterResponse;
import com.jira.sprint.dto.SubscriptionFrequency;
import com.jira.sprint.entity.SavedFilterEntity;
import com.jira.sprint.repository.SavedFilterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedFilterService {

    private final SavedFilterRepository savedFilterRepository;
    private final MessageSource messageSource;
    private final Map<UUID, FilterSubscriptionResponse> subscriptions = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${app.filter.system-filter-names:All Issues,My Issues,Reported by Me,Recently Updated,Open Issues}")
    private String systemFilterNamesStr;

    @Value("${app.filter.system-filter-jqls:,assignee = currentUser(),reporter = currentUser(),ORDER BY updated DESC,status NOT IN (Done\\, Closed)}")
    private String systemFilterJqlsStr;

    @PostConstruct
    void seedSystemFilters() {
        if (savedFilterRepository.count() > 0) return;
        String[] names = systemFilterNamesStr.split(",");
        String[] jqls = systemFilterJqlsStr.split(",");
        for (int i = 0; i < names.length; i++) {
            String jql = i < jqls.length ? jqls[i].trim() : "";
            seed(names[i].trim(), jql, true);
        }
    }

    private void seed(String name, String jql, boolean system) {
        savedFilterRepository.save(SavedFilterEntity.builder()
                .name(name)
                .jql(jql)
                .isShared(true)
                .shareType("SYSTEM")
                .isSystem(system)
                .favorite(false)
                .build());
    }

    @Transactional(readOnly = true)
    public List<SavedFilterResponse> getSavedFilters(UUID userId, String tab) {
        List<SavedFilterEntity> entities = switch (tab != null ? tab : "my") {
            case "system" -> savedFilterRepository.findByIsSystemTrue();
            case "shared" -> savedFilterRepository.findByIsSharedTrueAndIsSystemFalse();
            default -> {
                List<SavedFilterEntity> mine = userId != null
                        ? savedFilterRepository.findByOwnerIdAndIsSystemFalse(userId)
                        : List.of();
                yield mine;
            }
        };
        return entities.stream().map(this::toResponse).toList();
    }

    public boolean canAccessFilter(UUID userId, UUID filterId) {
        return savedFilterRepository.findById(filterId)
                .map(f -> Boolean.TRUE.equals(f.getIsSystem())
                        || Boolean.TRUE.equals(f.getIsShared())
                        || (userId != null && userId.equals(f.getOwnerId())))
                .orElse(false);
    }

    @Transactional
    public SavedFilterResponse createFilter(UUID userId, String name, String jql, boolean isShared) {
        if (userId == null) {
            throw new IllegalArgumentException(messageSource.getMessage("error.filter.auth.required", null, Locale.ENGLISH));
        }
        SavedFilterEntity entity = SavedFilterEntity.builder()
                .name(name)
                .jql(jql)
                .ownerId(userId)
                .isShared(isShared)
                .shareType(isShared ? "PROJECT" : "PRIVATE")
                .isSystem(false)
                .favorite(false)
                .usageCount(0)
                .build();
        return toResponse(savedFilterRepository.save(entity));
    }

    @Transactional
    public void deleteFilter(UUID filterId, UUID userId) {
        SavedFilterEntity f = savedFilterRepository.findById(filterId)
                .orElseThrow(() -> new IllegalArgumentException(messageSource.getMessage("error.filter.not.found", null, Locale.ENGLISH)));
        if (Boolean.TRUE.equals(f.getIsSystem())) {
            throw new IllegalArgumentException(messageSource.getMessage("error.filter.cannot.delete.system", null, Locale.ENGLISH));
        }
        if (userId == null || !userId.equals(f.getOwnerId())) {
            throw new IllegalArgumentException(messageSource.getMessage("error.filter.only.owner.can.delete", null, Locale.ENGLISH));
        }
        savedFilterRepository.delete(f);
    }

    @Transactional
    public SavedFilterResponse toggleFavorite(UUID filterId, UUID userId) {
        SavedFilterEntity entity = savedFilterRepository.findById(filterId)
                .orElseThrow(() -> new IllegalArgumentException(messageSource.getMessage("error.filter.not.found", null, Locale.ENGLISH)));
        if (!canAccessFilter(userId, filterId)) {
            throw new IllegalArgumentException(messageSource.getMessage("error.filter.not.allowed.to.modify", null, Locale.ENGLISH));
        }
        entity.setFavorite(!Boolean.TRUE.equals(entity.getFavorite()));
        return toResponse(savedFilterRepository.save(entity));
    }

    public List<FilterSubscriptionResponse> getSubscriptions(UUID userId) {
        return subscriptions.values().stream()
                .filter(s -> userId == null || userId.equals(s.getUserId()))
                .toList();
    }

    public FilterSubscriptionResponse createSubscription(UUID userId, String filterName, String jql,
                                                          SubscriptionFrequency frequency,
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
        return subscription;
    }

    public void deleteSubscription(UUID subscriptionId) {
        subscriptions.remove(subscriptionId);
    }

    public FilterSubscriptionResponse toggleSubscription(UUID subscriptionId) {
        FilterSubscriptionResponse subscription = subscriptions.get(subscriptionId);
        if (subscription != null) {
            subscription.setIsActive(!subscription.getIsActive());
        }
        return subscription;
    }

    private SavedFilterResponse toResponse(SavedFilterEntity e) {
        return SavedFilterResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .jql(e.getJql())
                .owner(e.getOwnerId() != null ? e.getOwnerId().toString() : "System")
                .isShared(Boolean.TRUE.equals(e.getIsShared()))
                .favorite(Boolean.TRUE.equals(e.getFavorite()))
                .shareType(e.getShareType())
                .isSystem(Boolean.TRUE.equals(e.getIsSystem()))
                .usageCount(e.getUsageCount())
                .lastUsed(e.getLastUsed())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
