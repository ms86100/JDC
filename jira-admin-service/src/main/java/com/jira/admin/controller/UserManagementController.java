package com.jira.admin.controller;

import com.jira.admin.entity.*;
import com.jira.admin.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User Management Controller - Enterprise user administration
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User Administration API")
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<Page<UserEntity>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userManagementService.getUsers(search, status, role, page, size));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserEntity> getUser(@PathVariable String userId) {
        return userManagementService.getUserById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserEntity> createUser(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(userManagementService.createUser(data));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user")
    public ResponseEntity<UserEntity> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(userManagementService.updateUser(userId, updates));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userManagementService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<Void> deactivateUser(@PathVariable String userId) {
        userManagementService.deactivateUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user")
    public ResponseEntity<Void> activateUser(@PathVariable String userId) {
        userManagementService.activateUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(userManagementService.getUserStatistics());
    }

    // ==================== Groups ====================

    @GetMapping("/groups")
    @Operation(summary = "Get all groups")
    public ResponseEntity<List<GroupEntity>> getGroups() {
        return ResponseEntity.ok(userManagementService.getGroups());
    }

    @PostMapping("/groups")
    @Operation(summary = "Create a new group")
    public ResponseEntity<GroupEntity> createGroup(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "JIRA_INTERNAL") String type) {
        return ResponseEntity.ok(userManagementService.createGroup(name, description, type));
    }

    @GetMapping("/groups/{groupId}/members")
    @Operation(summary = "Get all members of a group")
    public ResponseEntity<List<UserEntity>> getGroupMembers(@PathVariable String groupId) {
        return ResponseEntity.ok(userManagementService.getGroupMembers(groupId));
    }

    @PostMapping("/groups/{groupId}/members/{userId}")
    @Operation(summary = "Add user to group")
    public ResponseEntity<Void> addUserToGroup(
            @PathVariable String groupId,
            @PathVariable String userId) {
        userManagementService.addUserToGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    @Operation(summary = "Remove user from group")
    public ResponseEntity<Void> removeUserFromGroup(
            @PathVariable String groupId,
            @PathVariable String userId) {
        userManagementService.removeUserFromGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    // ==================== Project Roles ====================

    @GetMapping("/project-roles")
    @Operation(summary = "Get all project roles")
    public ResponseEntity<List<ProjectRoleEntity>> getProjectRoles() {
        return ResponseEntity.ok(userManagementService.getProjectRoles());
    }

    @PostMapping("/project-roles")
    @Operation(summary = "Create a new project role")
    public ResponseEntity<ProjectRoleEntity> createProjectRole(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam String roleType) {
        return ResponseEntity.ok(userManagementService.createProjectRole(name, description, roleType));
    }

    // ==================== Global Permissions ====================

    @GetMapping("/permissions")
    @Operation(summary = "Get all global permissions")
    public ResponseEntity<List<GlobalPermissionEntity>> getGlobalPermissions() {
        return ResponseEntity.ok(userManagementService.getGlobalPermissions());
    }

    @PostMapping("/permissions")
    @Operation(summary = "Grant global permission")
    public ResponseEntity<GlobalPermissionEntity> grantPermission(
            @RequestParam String permissionKey,
            @RequestParam String grantedToType,
            @RequestParam String grantedToId,
            @RequestParam(required = false) String grantedBy) {
        return ResponseEntity.ok(userManagementService.grantGlobalPermission(
                permissionKey, grantedToType, grantedToId, grantedBy));
    }

    @DeleteMapping("/permissions/{permissionId}")
    @Operation(summary = "Revoke global permission")
    public ResponseEntity<Void> revokePermission(@PathVariable String permissionId) {
        userManagementService.revokeGlobalPermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    // ==================== LDAP Configuration ====================

    @GetMapping("/ldap")
    @Operation(summary = "Get all LDAP configurations")
    public ResponseEntity<List<LdapConfigurationEntity>> getLdapConfigurations() {
        return ResponseEntity.ok(userManagementService.getLdapConfigurations());
    }

    @PostMapping("/ldap")
    @Operation(summary = "Create LDAP configuration")
    public ResponseEntity<LdapConfigurationEntity> createLdapConfiguration(
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(userManagementService.createLdapConfiguration(data));
    }

    @PostMapping("/ldap/{configId}/test")
    @Operation(summary = "Test LDAP connection")
    public ResponseEntity<Map<String, Object>> testLdapConnection(@PathVariable String configId) {
        userManagementService.testLdapConnection(configId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "LDAP connection test initiated"));
    }

    // ==================== Password Policy ====================

    @GetMapping("/password-policy")
    @Operation(summary = "Get all password policies")
    public ResponseEntity<List<PasswordPolicyEntity>> getPasswordPolicies() {
        return ResponseEntity.ok(userManagementService.getPasswordPolicies());
    }

    @PostMapping("/password-policy")
    @Operation(summary = "Create password policy")
    public ResponseEntity<PasswordPolicyEntity> createPasswordPolicy(
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(userManagementService.createPasswordPolicy(data));
    }

    // ==================== API Tokens ====================

    @GetMapping("/{userId}/tokens")
    @Operation(summary = "Get user's API tokens")
    public ResponseEntity<List<ApiTokenEntity>> getUserTokens(@PathVariable String userId) {
        return ResponseEntity.ok(userManagementService.getUserTokens(userId));
    }

    @PostMapping("/{userId}/tokens")
    @Operation(summary = "Create API token for user")
    public ResponseEntity<ApiTokenEntity> createApiToken(
            @PathVariable String userId,
            @RequestParam String tokenName,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(userManagementService.createApiToken(userId, tokenName, description));
    }

    @DeleteMapping("/tokens/{tokenId}")
    @Operation(summary = "Revoke API token")
    public ResponseEntity<Void> revokeApiToken(@PathVariable String tokenId) {
        userManagementService.revokeApiToken(tokenId);
        return ResponseEntity.noContent().build();
    }
}
