package com.jira.user.controller;

import com.jira.user.dto.*;
import com.jira.user.service.JiraUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rest/admin/1.0")
@RequiredArgsConstructor
public class AdminUserController {

    private final JiraUserManagementService userService;

    // ============ USERS ============

    @GetMapping("/users/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<UserResponse> users = userService.searchUsers(search, status, page, size);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity
                .created(URI.create("/rest/admin/1.0/users/" + user.getId()))
                .body(user);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // ============ GROUPS ============

    @GetMapping("/groups")
    public ResponseEntity<Page<GroupResponse>> searchGroups(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<GroupResponse> groups = userService.searchGroups(search, page, size);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(userService.getGroupById(groupId));
    }

    @GetMapping("/groups/name/{name}")
    public ResponseEntity<GroupResponse> getGroupByName(@PathVariable String name) {
        return ResponseEntity.ok(userService.getGroupByName(name));
    }

    @PostMapping("/groups")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        GroupResponse group = userService.createGroup(request);
        return ResponseEntity
                .created(URI.create("/rest/admin/1.0/groups/" + group.getId()))
                .body(group);
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId) {
        userService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    // ============ GROUP MEMBERS ============

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<List<UserResponse>> getGroupMembers(@PathVariable UUID groupId) {
        return ResponseEntity.ok(userService.getGroupMembers(groupId));
    }

    @PostMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> addUserToGroup(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        userService.addUserToGroup(userId, groupId);
        return ResponseEntity.created(URI.create("/rest/admin/1.0/groups/" + groupId + "/members/" + userId))
                .build();
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeUserFromGroup(
            @PathVariable UUID groupId,
            @PathVariable UUID userId) {
        userService.removeUserFromGroup(userId, groupId);
        return ResponseEntity.noContent().build();
    }
}