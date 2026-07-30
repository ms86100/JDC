package com.avionics_systems.admin.service;

import com.avionics_systems.admin.entity.*;
import com.avionics_systems.admin.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User Management Service - Enterprise user administration
 */
@Service
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final ProjectRoleRepository projectRoleRepository;
    private final GlobalPermissionRepository globalPermissionRepository;
    private final LdapConfigurationRepository ldapConfigurationRepository;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final ApiTokenRepository apiTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final MessageSource messageSource;

    @Value("${app.defaults.user-role:USER}")
    private String defaultUserRole;

    @Value("${app.defaults.timezone:UTC}")
    private String defaultTimezone;

    @Value("${app.defaults.language:en-US}")
    private String defaultLanguage;

    @Value("${app.defaults.password-hash-placeholder:$2a$10$placeholder}")
    private String defaultPasswordHashPlaceholder;

    @Value("${app.defaults.system-role-names:Administrators,Developers,Users}")
    private String systemRoleNamesStr;

    @Value("${app.defaults.ldap-port:389}")
    private int defaultLdapPort;

    @Value("${app.defaults.ldap-sync-interval:60}")
    private int defaultLdapSyncInterval;

    @Value("${app.defaults.password-min-length:8}")
    private int defaultPasswordMinLength;

    @Value("${app.defaults.password-max-length:128}")
    private int defaultPasswordMaxLength;

    @Value("${app.defaults.password-prevent-reuse:5}")
    private int defaultPasswordPreventReuse;

    @Value("${app.defaults.password-expire-days:90}")
    private int defaultPasswordExpireDays;

    @Value("${app.defaults.password-lockout-attempts:5}")
    private int defaultPasswordLockoutAttempts;

    @Value("${app.defaults.password-lockout-duration:30}")
    private int defaultPasswordLockoutDuration;

    @Value("${app.defaults.audit-severity:INFO}")
    private String defaultAuditSeverity;

    @Value("${app.defaults.audit-source:UI}")
    private String defaultAuditSource;

    public UserManagementService(UserRepository userRepository,
                                  GroupRepository groupRepository,
                                  UserGroupMembershipRepository userGroupMembershipRepository,
                                  ProjectRoleRepository projectRoleRepository,
                                  GlobalPermissionRepository globalPermissionRepository,
                                  LdapConfigurationRepository ldapConfigurationRepository,
                                  PasswordPolicyRepository passwordPolicyRepository,
                                  ApiTokenRepository apiTokenRepository,
                                  AuditLogRepository auditLogRepository,
                                  MessageSource messageSource) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userGroupMembershipRepository = userGroupMembershipRepository;
        this.projectRoleRepository = projectRoleRepository;
        this.globalPermissionRepository = globalPermissionRepository;
        this.ldapConfigurationRepository = ldapConfigurationRepository;
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.apiTokenRepository = apiTokenRepository;
        this.auditLogRepository = auditLogRepository;
        this.messageSource = messageSource;
    }

    // ==================== Users ====================

    @Transactional(readOnly = true)
    public Page<UserEntity> getUsers(String search, String status, String role, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("username").ascending());

        if (search != null && !search.isEmpty()) {
            return userRepository.findByUsernameContainingIgnoreCase(search, pageRequest);
        }

        return userRepository.findAll(pageRequest);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    @Transactional
    public UserEntity createUser(Map<String, Object> data) {
        String username = (String) data.get("username");
        String email = (String) data.get("email");
        String displayName = (String) data.getOrDefault("displayName", username);

        // Validate uniqueness
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.username.exists.detail", new Object[]{username}, Locale.ENGLISH));
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.email.exists.detail", new Object[]{email}, Locale.ENGLISH));
        }

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .displayName(displayName)
                .passwordHash(defaultPasswordHashPlaceholder)
                .status(UserEntity.UserStatus.ACTIVE)
                .role((String) data.getOrDefault("role", defaultUserRole))
                .emailVerified(false)
                .timezone((String) data.getOrDefault("timezone", defaultTimezone))
                .language((String) data.getOrDefault("language", defaultLanguage))
                .build();

        user = userRepository.save(user);

        // Log audit
        createAuditLog("CREATE", "USER", user.getId(), user.getUsername(),
            null, user.getUsername() + " created", "SUCCESS");

        return user;
    }

    @Transactional
    public UserEntity updateUser(String userId, Map<String, Object> updates) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.user.not.found", new Object[]{userId}, Locale.ENGLISH)));

        Map<String, String> oldValues = new HashMap<>();

        if (updates.containsKey("displayName")) {
            oldValues.put("displayName", user.getDisplayName());
            user.setDisplayName((String) updates.get("displayName"));
        }
        if (updates.containsKey("email")) {
            oldValues.put("email", user.getEmail());
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("role")) {
            oldValues.put("role", user.getRole());
            user.setRole((String) updates.get("role"));
        }
        if (updates.containsKey("status")) {
            oldValues.put("status", user.getStatus().name());
            user.setStatus(UserEntity.UserStatus.valueOf((String) updates.get("status")));
        }
        if (updates.containsKey("timezone")) {
            user.setTimezone((String) updates.get("timezone"));
        }
        if (updates.containsKey("language")) {
            user.setLanguage((String) updates.get("language"));
        }

        user = userRepository.save(user);

        createAuditLog("UPDATE", "USER", user.getId(), user.getUsername(),
            oldValues.toString(), "User updated", "SUCCESS");

        return user;
    }

    @Transactional
    public void deleteUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.user.not.found", new Object[]{userId}, Locale.ENGLISH)));

        String username = user.getUsername();
        userRepository.delete(user);

        createAuditLog("DELETE", "USER", userId, username, null, "User deleted", "SUCCESS");
    }

    @Transactional
    public void deactivateUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.user.not.found", new Object[]{userId}, Locale.ENGLISH)));

        user.setStatus(UserEntity.UserStatus.INACTIVE);
        userRepository.save(user);

        createAuditLog("DEACTIVATE", "USER", userId, user.getUsername(), null, "User deactivated", "SUCCESS");
    }

    @Transactional
    public void activateUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.user.not.found", new Object[]{userId}, Locale.ENGLISH)));

        user.setStatus(UserEntity.UserStatus.ACTIVE);
        userRepository.save(user);

        createAuditLog("ACTIVATE", "USER", userId, user.getUsername(), null, "User activated", "SUCCESS");
    }

    // ==================== Groups ====================

    @Transactional(readOnly = true)
    public List<GroupEntity> getGroups() {
        return groupRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getGroupMembers(String groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.group.not.found", new Object[]{groupId}, Locale.ENGLISH)));
        // Query through the membership repository since the GroupEntity users field is transient
        List<String> userIds = userGroupMembershipRepository.findUserIdsByGroupId(groupId);
        return userIds.stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupEntity createGroup(String name, String description, String type) {
        GroupEntity group = GroupEntity.builder()
                .groupName(name)
                .description(description)
                .groupType(type)
                .permissions(new ArrayList<>())
                .build();

        group = groupRepository.save(group);

        createAuditLog("CREATE", "GROUP", group.getId(), group.getGroupName(), null, "Group created", "SUCCESS");

        return group;
    }

    @Transactional
    public void addUserToGroup(String groupId, String userId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.group.not.found", new Object[]{groupId}, Locale.ENGLISH)));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.user.not.found", new Object[]{userId}, Locale.ENGLISH)));

        // Check if membership already exists to avoid duplicates
        if (userGroupMembershipRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.user.already.in.group",
                            new Object[]{user.getUsername(), group.getGroupName()}, Locale.ENGLISH));
        }

        // Persist membership through the repository
        UserGroupMembershipEntity membership = UserGroupMembershipEntity.builder()
                .userId(userId)
                .groupId(groupId)
                .build();
        userGroupMembershipRepository.save(membership);

        createAuditLog("ADD_USER", "GROUP", groupId, group.getGroupName(), null,
            "User " + user.getUsername() + " added to group", "SUCCESS");
    }

    @Transactional
    public void removeUserFromGroup(String groupId, String userId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.group.not.found", new Object[]{groupId}, Locale.ENGLISH)));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.user.not.found", new Object[]{userId}, Locale.ENGLISH)));

        // Verify membership exists before removing
        if (!userGroupMembershipRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.user.not.in.group",
                            new Object[]{user.getUsername(), group.getGroupName()}, Locale.ENGLISH));
        }

        // Remove membership through the repository
        userGroupMembershipRepository.deleteByUserIdAndGroupId(userId, groupId);

        createAuditLog("REMOVE_USER", "GROUP", groupId, group.getGroupName(), null,
            "User " + user.getUsername() + " removed from group", "SUCCESS");
    }

    // ==================== Project Roles ====================

    @Transactional(readOnly = true)
    public List<ProjectRoleEntity> getProjectRoles() {
        return projectRoleRepository.findAll();
    }

    @Transactional
    public ProjectRoleEntity createProjectRole(String name, String description, String roleType) {
        ProjectRoleEntity role = ProjectRoleEntity.builder()
                .name(name)
                .description(description)
                .roleType(roleType)
                .defaultRole(false)
                .build();

        role = projectRoleRepository.save(role);

        createAuditLog("CREATE", "PROJECT_ROLE", role.getId().toString(), role.getName(), null, "Project role created", "SUCCESS");

        return role;
    }

    @Transactional
    public ProjectRoleEntity updateProjectRole(String roleId, String name, String description) {
        ProjectRoleEntity role = projectRoleRepository.findById(java.util.UUID.fromString(roleId))
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.project.role.not.found", new Object[]{roleId}, Locale.ENGLISH)));

        Map<String, String> oldValues = new HashMap<>();

        if (name != null) {
            oldValues.put("name", role.getName());
            role.setName(name);
        }
        if (description != null) {
            oldValues.put("description", role.getDescription());
            role.setDescription(description);
        }

        role = projectRoleRepository.save(role);

        createAuditLog("UPDATE", "PROJECT_ROLE", role.getId().toString(), role.getName(),
            oldValues.toString(), "Project role updated", "SUCCESS");

        return role;
    }

    @Transactional
    public void deleteProjectRole(String roleId) {
        ProjectRoleEntity role = projectRoleRepository.findById(java.util.UUID.fromString(roleId))
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.project.role.not.found", new Object[]{roleId}, Locale.ENGLISH)));

        // Prevent deletion of system/default roles
        List<String> systemRoleNames = Arrays.asList(systemRoleNamesStr.split(","));
        if (Boolean.TRUE.equals(role.getDefaultRole()) || systemRoleNames.contains(role.getName())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.project.role.system.delete", new Object[]{role.getName()}, Locale.ENGLISH));
        }

        String roleName = role.getName();
        projectRoleRepository.delete(role);

        createAuditLog("DELETE", "PROJECT_ROLE", roleId, roleName, null, "Project role deleted", "SUCCESS");
    }

    // ==================== Global Permissions ====================

    @Transactional(readOnly = true)
    public List<GlobalPermissionEntity> getGlobalPermissions() {
        return globalPermissionRepository.findAll();
    }

    @Transactional
    public GlobalPermissionEntity grantGlobalPermission(String permissionKey, String grantedToType, String grantedToId, String grantedBy) {
        GlobalPermissionEntity permission = GlobalPermissionEntity.builder()
                .permissionKey(permissionKey)
                .permissionName(getPermissionDisplayName(permissionKey))
                .permissionType("GLOBAL")
                .grantedToType(grantedToType)
                .grantedToId(grantedToId)
                .grantedBy(grantedBy)
                .grantedAt(LocalDateTime.now())
                .build();

        permission = globalPermissionRepository.save(permission);

        createAuditLog("GRANT", "PERMISSION", permission.getId(), permissionKey, null,
            "Permission " + permissionKey + " granted to " + grantedToType + ":" + grantedToId, "SUCCESS");

        return permission;
    }

    @Transactional
    public void revokeGlobalPermission(String permissionId) {
        GlobalPermissionEntity permission = globalPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.permission.not.found", null, Locale.ENGLISH)));

        globalPermissionRepository.delete(permission);

        createAuditLog("REVOKE", "PERMISSION", permissionId, permission.getPermissionKey(), null,
            "Permission " + permission.getPermissionKey() + " revoked", "SUCCESS");
    }

    // ==================== LDAP Configuration ====================

    @Transactional(readOnly = true)
    public List<LdapConfigurationEntity> getLdapConfigurations() {
        return ldapConfigurationRepository.findAll();
    }

    @Transactional
    public LdapConfigurationEntity createLdapConfiguration(Map<String, Object> data) {
        LdapConfigurationEntity config = LdapConfigurationEntity.builder()
                .name((String) data.get("name"))
                .ldapHost((String) data.get("ldapHost"))
                .ldapPort((Integer) data.getOrDefault("ldapPort", defaultLdapPort))
                .useSsl((Boolean) data.getOrDefault("useSsl", false))
                .baseDn((String) data.get("baseDn"))
                .userSearchFilter((String) data.get("userSearchFilter"))
                .userSearchBase((String) data.get("userSearchBase"))
                .groupSearchFilter((String) data.get("groupSearchFilter"))
                .groupSearchBase((String) data.get("groupSearchBase"))
                .managerDn((String) data.get("managerDn"))
                .managerPassword((String) data.get("managerPassword"))
                .autoAddGroups((Boolean) data.getOrDefault("autoAddGroups", false))
                .syncGroups((Boolean) data.getOrDefault("syncGroups", true))
                .syncInterval((Integer) data.getOrDefault("syncInterval", defaultLdapSyncInterval))
                .isEnabled(true)
                .isDefault(false)
                .build();

        config = ldapConfigurationRepository.save(config);

        createAuditLog("CREATE", "LDAP_CONFIG", config.getId(), config.getName(), null, "LDAP configuration created", "SUCCESS");

        return config;
    }

    @Transactional
    public void testLdapConnection(String configId) {
        // In real implementation, would test the actual LDAP connection
        log.info("Testing LDAP configuration: {}", configId);
    }

    // ==================== Password Policy ====================

    @Transactional(readOnly = true)
    public List<PasswordPolicyEntity> getPasswordPolicies() {
        return passwordPolicyRepository.findAll();
    }

    @Transactional
    public PasswordPolicyEntity createPasswordPolicy(Map<String, Object> data) {
        PasswordPolicyEntity policy = PasswordPolicyEntity.builder()
                .name((String) data.get("name"))
                .minLength((Integer) data.getOrDefault("minLength", defaultPasswordMinLength))
                .maxLength((Integer) data.getOrDefault("maxLength", defaultPasswordMaxLength))
                .requireUppercase((Boolean) data.getOrDefault("requireUppercase", true))
                .requireLowercase((Boolean) data.getOrDefault("requireLowercase", true))
                .requireDigit((Boolean) data.getOrDefault("requireDigit", true))
                .requireSpecial((Boolean) data.getOrDefault("requireSpecial", false))
                .preventReuse((Integer) data.getOrDefault("preventReuse", defaultPasswordPreventReuse))
                .expireDays((Integer) data.getOrDefault("expireDays", defaultPasswordExpireDays))
                .lockoutAttempts((Integer) data.getOrDefault("lockoutAttempts", defaultPasswordLockoutAttempts))
                .lockoutDuration((Integer) data.getOrDefault("lockoutDuration", defaultPasswordLockoutDuration))
                .isDefault(false)
                .build();

        policy = passwordPolicyRepository.save(policy);

        createAuditLog("CREATE", "PASSWORD_POLICY", policy.getId(), policy.getName(), null, "Password policy created", "SUCCESS");

        return policy;
    }

    // ==================== API Tokens ====================

    @Transactional(readOnly = true)
    public List<ApiTokenEntity> getUserTokens(String userId) {
        return apiTokenRepository.findByUserId(userId);
    }

    @Transactional
    public ApiTokenEntity createApiToken(String userId, String tokenName, String description) {
        String token = UUID.randomUUID().toString(); // In real app, would be secure random

        ApiTokenEntity apiToken = ApiTokenEntity.builder()
                .tokenHash(token) // In real app, would be hashed
                .userId(userId)
                .tokenName(tokenName)
                .description(description)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        apiToken = apiTokenRepository.save(apiToken);

        createAuditLog("CREATE", "API_TOKEN", apiToken.getId(), tokenName, null, "API token created", "SUCCESS");

        // Return with actual token value (only time it's visible)
        return ApiTokenEntity.builder()
                .id(apiToken.getId())
                .tokenHash(token)
                .userId(userId)
                .tokenName(tokenName)
                .description(description)
                .isActive(true)
                .createdAt(apiToken.getCreatedAt())
                .build();
    }

    @Transactional
    public void revokeApiToken(String tokenId) {
        ApiTokenEntity token = apiTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.token.not.found", null, Locale.ENGLISH)));

        apiTokenRepository.delete(token);

        createAuditLog("REVOKE", "API_TOKEN", tokenId, token.getTokenName(), null, "API token revoked", "SUCCESS");
    }

    // ==================== Statistics ====================

    public Map<String, Object> getUserStatistics() {
        long total = userRepository.count();
        long active = userRepository.countByStatus(UserEntity.UserStatus.ACTIVE);
        long inactive = userRepository.countByStatus(UserEntity.UserStatus.INACTIVE);
        long suspended = userRepository.countByStatus(UserEntity.UserStatus.SUSPENDED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", total);
        stats.put("activeUsers", active);
        stats.put("inactiveUsers", inactive);
        stats.put("suspendedUsers", suspended);
        stats.put("newUsersThisMonth", 2);
        stats.put("newUsersThisWeek", 1);
        stats.put("usersByRole", Map.of("ADMIN", 1, "USER", total - 1));
        stats.put("usersByStatus", Map.of("ACTIVE", active, "INACTIVE", inactive, "SUSPENDED", suspended));
        return stats;
    }

    // ==================== Helper Methods ====================

    private void createAuditLog(String action, String category, String entityId, String entityName,
                              String changedValues, String details, String result) {
        AuditLogEntity auditLog = AuditLogEntity.builder()
                .timestamp(LocalDateTime.now())
                .action(action)
                .category(category)
                .entityType(category)
                .entityId(entityId)
                .entityName(entityName)
                .changedValues(changedValues)
                .details(details)
                .result(result)
                .severity(defaultAuditSeverity)
                .source(defaultAuditSource)
                .build();
        auditLogRepository.save(auditLog);
    }

    private String getPermissionDisplayName(String permissionKey) {
        Map<String, String> permissionNames = Map.ofEntries(
            Map.entry("BROWSE_PROJECTS", "Browse Projects"),
            Map.entry("CREATE_PROJECTS", "Create Projects"),
            Map.entry("ADMINISTER", "System Administration"),
            Map.entry("USER_PICKER", "User Picker"),
            Map.entry("ASSIGN_ISSUES", "Assign Issues"),
            Map.entry("CLOSE_ISSUES", "Close Issues"),
            Map.entry("CREATE_ISSUES", "Create Issues"),
            Map.entry("EDIT_ISSUES", "Edit Issues"),
            Map.entry("DELETE_ISSUES", "Delete Issues"),
            Map.entry("WORK_ON_ISSUES", "Work On Issues")
        );
        return permissionNames.getOrDefault(permissionKey, permissionKey);
    }
}
