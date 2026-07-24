package com.jira.user.service;

import com.jira.user.entity.CwdGroup;
import com.jira.user.entity.CwdUser;
import com.jira.user.entity.Directory;
import com.jira.user.entity.DirectorySyncLog;
import com.jira.user.exception.ResourceNotFoundException;
import com.jira.user.repository.CwdGroupRepository;
import com.jira.user.repository.CwdUserRepository;
import com.jira.user.repository.DirectoryRepository;
import com.jira.user.repository.DirectorySyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LdapSyncService {

    private final DirectoryRepository directoryRepository;
    private final DirectorySyncLogRepository syncLogRepository;
    private final CwdUserRepository cwdUserRepository;
    private final CwdGroupRepository cwdGroupRepository;

    @Transactional
    public DirectorySyncLog syncDirectory(UUID directoryId) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Directory not found: " + directoryId));

        if (!"LDAP".equalsIgnoreCase(directory.getDirectoryType()) &&
                !"AD".equalsIgnoreCase(directory.getDirectoryType())) {
            throw new IllegalArgumentException("Directory is not an LDAP/AD directory: " + directoryId);
        }

        if (directory.getServerUrl() == null || directory.getServerUrl().isBlank()) {
            throw new IllegalArgumentException("LDAP server URL is not configured for directory: " + directoryId);
        }

        if ("SYNCING".equals(directory.getSyncStatus())) {
            log.warn("Sync already in progress for directory: {}", directoryId);
            throw new IllegalStateException("Sync already in progress for directory: " + directoryId);
        }

        directory.setSyncStatus("SYNCING");
        directoryRepository.save(directory);

        DirectorySyncLog syncLog = DirectorySyncLog.builder()
                .directoryId(directoryId)
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build();
        syncLog = syncLogRepository.save(syncLog);

        try {
            LdapTemplate ldapTemplate = buildLdapTemplate(directory);

            int usersAdded = 0;
            int usersUpdated = 0;
            int groupsSynced = 0;

            List<LdapUserRecord> ldapUsers = fetchLdapUsers(ldapTemplate, directory);
            Set<String> syncedUserNames = new HashSet<>();

            for (LdapUserRecord ldapUser : ldapUsers) {
                try {
                    Optional<CwdUser> existing = cwdUserRepository
                            .findByLowerUserNameAndDirectoryId(ldapUser.userName.toLowerCase(), directoryId);

                    if (existing.isPresent()) {
                        CwdUser user = existing.get();
                        boolean changed = false;

                        if (!Objects.equals(user.getEmailAddress(), ldapUser.email)) {
                            user.setEmailAddress(ldapUser.email);
                            changed = true;
                        }
                        if (!Objects.equals(user.getDisplayName(), ldapUser.displayName)) {
                            user.setDisplayName(ldapUser.displayName);
                            changed = true;
                        }
                        if (!Objects.equals(user.getFirstName(), ldapUser.firstName)) {
                            user.setFirstName(ldapUser.firstName);
                            changed = true;
                        }
                        if (!Objects.equals(user.getLastName(), ldapUser.lastName)) {
                            user.setLastName(ldapUser.lastName);
                            changed = true;
                        }
                        if (!Objects.equals(user.getExternalId(), ldapUser.externalId)) {
                            user.setExternalId(ldapUser.externalId);
                            changed = true;
                        }

                        if (changed) {
                            cwdUserRepository.save(user);
                            usersUpdated++;
                        }
                    } else {
                        CwdUser newUser = CwdUser.builder()
                                .directoryId(directoryId)
                                .userName(ldapUser.userName)
                                .lowerUserName(ldapUser.userName.toLowerCase())
                                .emailAddress(ldapUser.email)
                                .displayName(ldapUser.displayName)
                                .firstName(ldapUser.firstName)
                                .lastName(ldapUser.lastName)
                                .externalId(ldapUser.externalId)
                                .active(true)
                                .build();
                        cwdUserRepository.save(newUser);
                        usersAdded++;
                    }

                    syncedUserNames.add(ldapUser.userName.toLowerCase());
                } catch (Exception e) {
                    log.error("Failed to sync LDAP user {}: {}", ldapUser.userName, e.getMessage());
                }
            }

            List<LdapGroupRecord> ldapGroups = fetchLdapGroups(ldapTemplate, directory);
            for (LdapGroupRecord ldapGroup : ldapGroups) {
                try {
                    Optional<CwdGroup> existing = cwdGroupRepository
                            .findByLowerGroupNameAndDirectoryId(ldapGroup.groupName.toLowerCase(), directoryId);

                    if (existing.isEmpty()) {
                        CwdGroup newGroup = CwdGroup.builder()
                                .directoryId(directoryId)
                                .groupName(ldapGroup.groupName)
                                .lowerGroupName(ldapGroup.groupName.toLowerCase())
                                .description(ldapGroup.description)
                                .active(true)
                                .isGlobal(false)
                                .isSystem(false)
                                .build();
                        cwdGroupRepository.save(newGroup);
                    } else {
                        CwdGroup group = existing.get();
                        if (!Objects.equals(group.getDescription(), ldapGroup.description)) {
                            group.setDescription(ldapGroup.description);
                            cwdGroupRepository.save(group);
                        }
                    }
                    groupsSynced++;
                } catch (Exception e) {
                    log.error("Failed to sync LDAP group {}: {}", ldapGroup.groupName, e.getMessage());
                }
            }

            syncLog.setUsersAdded(usersAdded);
            syncLog.setUsersUpdated(usersUpdated);
            syncLog.setGroupsSynced(groupsSynced);
            syncLog.setStatus("COMPLETED");
            syncLog.setCompletedAt(LocalDateTime.now());
            syncLogRepository.save(syncLog);

            directory.setSyncStatus("IDLE");
            directory.setLastSyncAt(LocalDateTime.now());
            directoryRepository.save(directory);

            log.info("LDAP sync completed for directory {}: {} added, {} updated, {} groups",
                    directoryId, usersAdded, usersUpdated, groupsSynced);

            return syncLog;

        } catch (Exception e) {
            log.error("LDAP sync failed for directory {}: {}", directoryId, e.getMessage(), e);

            syncLog.setStatus("FAILED");
            syncLog.setErrors(e.getMessage());
            syncLog.setCompletedAt(LocalDateTime.now());
            syncLogRepository.save(syncLog);

            directory.setSyncStatus("FAILED");
            directoryRepository.save(directory);

            return syncLog;
        }
    }

    private LdapTemplate buildLdapTemplate(Directory directory) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(directory.getServerUrl());
        contextSource.setBase(directory.getBaseDn());
        contextSource.setUserDn(directory.getBindDn());

        if (directory.getEncryptedBindPassword() != null) {
            contextSource.setPassword(directory.getEncryptedBindPassword());
        }

        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }

    private List<LdapUserRecord> fetchLdapUsers(LdapTemplate ldapTemplate, Directory directory) {
        String searchBase = directory.getUserSearchBase() != null ? directory.getUserSearchBase() : "";
        String filter = directory.getUserSearchFilter() != null ? directory.getUserSearchFilter() : "(objectClass=person)";

        try {
            return ldapTemplate.search(
                    LdapQueryBuilder.query()
                            .base(searchBase)
                            .filter(filter),
                    (AttributesMapper<LdapUserRecord>) attrs -> mapToUserRecord(attrs)
            );
        } catch (Exception e) {
            log.error("Failed to fetch LDAP users: {}", e.getMessage());
            return List.of();
        }
    }

    private List<LdapGroupRecord> fetchLdapGroups(LdapTemplate ldapTemplate, Directory directory) {
        String searchBase = directory.getGroupSearchBase() != null ? directory.getGroupSearchBase() : "";
        String filter = directory.getGroupSearchFilter() != null ? directory.getGroupSearchFilter() : "(objectClass=group)";

        try {
            return ldapTemplate.search(
                    LdapQueryBuilder.query()
                            .base(searchBase)
                            .filter(filter),
                    (AttributesMapper<LdapGroupRecord>) attrs -> mapToGroupRecord(attrs)
            );
        } catch (Exception e) {
            log.error("Failed to fetch LDAP groups: {}", e.getMessage());
            return List.of();
        }
    }

    private LdapUserRecord mapToUserRecord(Attributes attrs) throws NamingException {
        LdapUserRecord record = new LdapUserRecord();
        record.userName = getAttr(attrs, "sAMAccountName");
        if (record.userName == null) {
            record.userName = getAttr(attrs, "uid");
        }
        record.email = getAttr(attrs, "mail");
        record.firstName = getAttr(attrs, "givenName");
        record.lastName = getAttr(attrs, "sn");
        record.displayName = getAttr(attrs, "displayName");
        if (record.displayName == null && record.firstName != null) {
            record.displayName = record.firstName + (record.lastName != null ? " " + record.lastName : "");
        }
        record.externalId = getAttr(attrs, "objectGUID");
        if (record.externalId == null) {
            record.externalId = getAttr(attrs, "entryUUID");
        }
        return record;
    }

    private LdapGroupRecord mapToGroupRecord(Attributes attrs) throws NamingException {
        LdapGroupRecord record = new LdapGroupRecord();
        record.groupName = getAttr(attrs, "cn");
        record.description = getAttr(attrs, "description");
        return record;
    }

    private String getAttr(Attributes attrs, String name) throws NamingException {
        return attrs.get(name) != null ? (String) attrs.get(name).get() : null;
    }

    private static class LdapUserRecord {
        String userName;
        String email;
        String firstName;
        String lastName;
        String displayName;
        String externalId;
    }

    private static class LdapGroupRecord {
        String groupName;
        String description;
    }
}
