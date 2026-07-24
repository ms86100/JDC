package com.jira.sprint.controller;

import com.jira.sprint.dto.FilterSubscriptionResponse;
import com.jira.sprint.dto.SavedFilterResponse;
import com.jira.sprint.dto.SubscriptionFrequency;
import com.jira.sprint.service.SavedFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/filters")
@RequiredArgsConstructor
@Tag(name = "Saved Filters", description = "Saved filters and subscriptions API")
public class SavedFilterController {

    private final SavedFilterService savedFilterService;

    @GetMapping
    @Operation(summary = "Get saved filters", description = "Get saved filters for a user")
    public ResponseEntity<List<SavedFilterResponse>> getSavedFilters(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "my") String tab) {
        return ResponseEntity.ok(savedFilterService.getSavedFilters(userId, tab));
    }

    @PostMapping
    @Operation(summary = "Create filter", description = "Create a new saved filter")
    public ResponseEntity<SavedFilterResponse> createFilter(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") UUID userId) {
        String name = (String) request.get("name");
        String jql = (String) request.get("jql");
        Boolean isShared = (Boolean) request.getOrDefault("isShared", false);

        SavedFilterResponse filter = savedFilterService.createFilter(userId, name, jql, isShared);
        return new ResponseEntity<>(filter, HttpStatus.CREATED);
    }

    @DeleteMapping("/{filterId}")
    @Operation(summary = "Delete filter", description = "Delete a saved filter")
    public ResponseEntity<Void> deleteFilter(
            @PathVariable UUID filterId,
            @RequestHeader("X-User-Id") UUID userId) {
        savedFilterService.deleteFilter(filterId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{filterId}/favorite")
    @Operation(summary = "Toggle favorite", description = "Toggle filter favorite status")
    public ResponseEntity<SavedFilterResponse> toggleFavorite(
            @PathVariable UUID filterId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(savedFilterService.toggleFavorite(filterId, userId));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Get subscriptions", description = "Get filter subscriptions for a user")
    public ResponseEntity<List<FilterSubscriptionResponse>> getSubscriptions(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(savedFilterService.getSubscriptions(userId));
    }

    @PostMapping("/subscriptions")
    @Operation(summary = "Create subscription", description = "Create a new filter subscription")
    public ResponseEntity<FilterSubscriptionResponse> createSubscription(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") UUID userId) {
        String filterName = (String) request.get("filterName");
        String jql = (String) request.get("jql");
        String frequencyStr = (String) request.getOrDefault("frequency", "INSTANT");
        Boolean emailNotification = (Boolean) request.getOrDefault("emailNotification", true);

        SubscriptionFrequency frequency =
            SubscriptionFrequency.valueOf(frequencyStr);

        FilterSubscriptionResponse subscription = savedFilterService.createSubscription(
                userId, filterName, jql, frequency, emailNotification);
        return new ResponseEntity<>(subscription, HttpStatus.CREATED);
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Delete subscription", description = "Delete a filter subscription")
    public ResponseEntity<Void> deleteSubscription(@PathVariable UUID subscriptionId) {
        savedFilterService.deleteSubscription(subscriptionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subscriptions/{subscriptionId}/toggle")
    @Operation(summary = "Toggle subscription", description = "Toggle subscription active status")
    public ResponseEntity<FilterSubscriptionResponse> toggleSubscription(@PathVariable UUID subscriptionId) {
        return ResponseEntity.ok(savedFilterService.toggleSubscription(subscriptionId));
    }
}