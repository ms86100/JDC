package com.jira.user.service;

import com.jira.user.dto.*;
import com.jira.user.entity.*;
import com.jira.user.exception.DuplicateResourceException;
import com.jira.user.exception.ResourceNotFoundException;
import com.jira.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraUserManagementService {

    private final CwdUserRepository userRepository;
    private final CwdGroupRepository groupRepository;
    private final CwdMembershipRepository membershipRepository;
    private final DirectoryRepository directoryRepository;

    @Value("${app.defaults.directory-id:00000000-0000-0000-0000-000000000001}")
    private UUID defaultDirectoryId;

    @Value("${app.defaults.active-status:ACTIVE}")
    private String activeStatus;

    @Value("${app.defaults.membership-type:GROUP_USER}")
    private String membershipType;

    @Value("${app.defaults.admin-group-keyword:administrators}")
    private String adminGroupKeyword;

    @Value("${app.defaults.software-group-keyword:software}")
    private String softwareGroupKeyword;

    @Value("${app.defaults.directory-name-fallback:Internal Directory}")
    private String defaultDirectoryName;

    @Value("${app.defaults.default-application:Platform Software}")
    private String defaultApplication;

    @Value("${app.defaults.default-login-count:0}")
    private int defaultLoginCount;

    @Value("${app.defaults.generated-password-length:12}")
    private int generatedPasswordLength;

    // ============ USER OPERATIONS ============

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<CwdUser> users;
        if (search != null && !search.isBlank()) {
            if (status != null && !status.isBlank()) {
                boolean isActive = activeStatus.equalsIgnoreCase(status);
                users = userRepository.searchUsersByStatus(defaultDirectoryId, search, isActive, pageable);
            } else {
                users = userRepository.searchUsers(defaultDirectoryId, search, pageable);
            }
        } else {
            users = userRepository.findByDirectoryIdAndActive(defaultDirectoryId, true, pageable);
        }

        return users.map(this::mapToUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        CwdUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByLowerUserNameAndDirectoryId(request.getUserName().toLowerCase(), defaultDirectoryId)) {
            throw new DuplicateResourceException("Username already exists: " + request.getUserName());
        }

        if (userRepository.existsByEmailAddressAndDirectoryId(request.getEmail(), defaultDirectoryId)) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        String[] nameParts = splitName(request.getFullName());

        CwdUser user = CwdUser.builder()
                .directoryId(defaultDirectoryId)
                .userName(request.getUserName())
                .emailAddress(request.getEmail())
                .displayName(request.getFullName())
                .firstName(nameParts[0])
                .lastName(nameParts[1])
                .passwordHash(hashPassword(request.getPassword() != null ? request.getPassword() : generatePassword()))
                .active(true)
                .lowerUserName(request.getUserName().toLowerCase())
                .build();

        user = userRepository.save(user);
        log.info("Created user: {} ({})", user.getUserName(), user.getId());

        return mapToUserResponse(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        CwdUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        membershipRepository.deleteAllByChildIdAndMembershipType(userId, membershipType);
        userRepository.delete(user);
        log.info("Deleted user: {} ({})", user.getUserName(), userId);
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        CwdUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getEmail() != null) {
            user.setEmailAddress(request.getEmail());
        }
        if (request.getFullName() != null) {
            user.setDisplayName(request.getFullName());
            String[] nameParts = splitName(request.getFullName());
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts[1]);
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        user = userRepository.save(user);
        log.info("Updated user: {} ({})", user.getUserName(), user.getId());

        return mapToUserResponse(user);
    }

    // ============ GROUP OPERATIONS ============

    @Transactional(readOnly = true)
    public Page<GroupResponse> searchGroups(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<CwdGroup> groups;
        if (search != null && !search.isBlank()) {
            groups = groupRepository.searchGroups(defaultDirectoryId, search, pageable);
        } else {
            groups = groupRepository.findByDirectoryIdAndActive(defaultDirectoryId, true, pageable);
        }

        return groups.map(this::mapToGroupResponse);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(UUID groupId) {
        CwdGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        return mapToGroupResponse(group);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupByName(String name) {
        CwdGroup group = groupRepository.findByLowerGroupNameAndDirectoryId(name.toLowerCase(), defaultDirectoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + name));
        return mapToGroupResponse(group);
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request) {
        if (groupRepository.existsByLowerGroupNameAndDirectoryId(request.getName().toLowerCase(), defaultDirectoryId)) {
            throw new DuplicateResourceException("Group already exists: " + request.getName());
        }

        CwdGroup group = CwdGroup.builder()
                .directoryId(defaultDirectoryId)
                .groupName(request.getName())
                .description(request.getDescription())
                .active(true)
                .lowerGroupName(request.getName().toLowerCase())
                .isGlobal(false)
                .isSystem(false)
                .build();

        group = groupRepository.save(group);
        log.info("Created group: {} ({})", group.getGroupName(), group.getId());

        return mapToGroupResponse(group);
    }

    @Transactional
    public GroupResponse updateGroup(UUID groupId, CreateGroupRequest request) {
        CwdGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (request.getName() != null) {
            group.setGroupName(request.getName());
            group.setLowerGroupName(request.getName().toLowerCase());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }

        group = groupRepository.save(group);
        log.info("Updated group: {} ({})", group.getGroupName(), group.getId());

        return mapToGroupResponse(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        CwdGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        membershipRepository.findByParentIdAndMembershipType(groupId, membershipType)
                .forEach(m -> membershipRepository.delete(m));

        groupRepository.delete(group);
        log.info("Deleted group: {} ({})", group.getGroupName(), groupId);
    }

    // ============ MEMBERSHIP OPERATIONS ============

    @Transactional(readOnly = true)
    public List<UserResponse> getGroupMembers(UUID groupId) {
        return membershipRepository.findByParentIdAndMembershipType(groupId, membershipType)
                .stream()
                .map(m -> userRepository.findById(m.getChildId()).orElse(null))
                .filter(u -> u != null)
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addUserToGroup(UUID userId, UUID groupId) {
        if (!membershipRepository.existsByParentIdAndChildIdAndMembershipType(groupId, userId, membershipType)) {
            CwdMembership membership = CwdMembership.builder()
                    .parentId(groupId)
                    .childId(userId)
                    .membershipType(membershipType)
                    .build();
            membershipRepository.save(membership);
            log.info("Added user {} to group {}", userId, groupId);
        }
    }

    @Transactional
    public void removeUserFromGroup(UUID userId, UUID groupId) {
        membershipRepository.deleteByParentIdAndChildIdAndMembershipType(groupId, userId, membershipType);
        log.info("Removed user {} from group {}", userId, groupId);
    }

    // ============ MAPPING HELPERS ============

    private UserResponse mapToUserResponse(CwdUser user) {
        List<CwdGroup> groups = groupRepository.findGroupsByUserId(user.getId());

        List<UserResponse.GroupInfo> groupInfos = groups.stream()
                .map(g -> UserResponse.GroupInfo.builder()
                        .id(g.getId())
                        .name(g.getGroupName())
                        .isAdmin(g.isSystem() && g.getGroupName().contains(adminGroupKeyword))
                        .isJiraSoftware(g.getGroupName().contains(softwareGroupKeyword))
                        .build())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .emailAddress(user.getEmailAddress())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(user.isActive())
                .createdDate(user.getCreatedDate())
                .updatedDate(user.getUpdatedDate())
                .directoryId(user.getDirectoryId())
                .directoryName(directoryRepository.findById(user.getDirectoryId())
                        .map(d -> d.getDirectoryName()).orElse(defaultDirectoryName))
                .groups(groupInfos)
                .applications(List.of(defaultApplication))
                .loginInfo(UserResponse.LoginInfo.builder()
                        .loginCount(defaultLoginCount)
                        .lastLogin(user.getLastAuthDate())
                        .build())
                .build();
    }

    private GroupResponse mapToGroupResponse(CwdGroup group) {
        int userCount = membershipRepository.countByParentIdAndType(group.getId(), membershipType);

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getGroupName())
                .description(group.getDescription())
                .active(group.isActive())
                .createdDate(group.getCreatedDate())
                .isSystem(group.isSystem())
                .userCount(userCount)
                .permissionSchemes(List.of())
                .notificationSchemes(List.of())
                .securitySchemes(List.of())
                .build();
    }

    private String[] splitName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return new String[]{parts[0], parts[parts.length - 1]};
        }
        return new String[]{fullName, ""};
    }

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return UUID.randomUUID().toString().substring(0, generatedPasswordLength);
    }

}