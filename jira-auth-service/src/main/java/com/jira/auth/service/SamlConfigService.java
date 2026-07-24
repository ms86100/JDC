package com.jira.auth.service;

import com.jira.auth.entity.Role;
import com.jira.auth.entity.SamlConfiguration;
import com.jira.auth.entity.User;
import com.jira.auth.repository.RoleRepository;
import com.jira.auth.repository.SamlConfigurationRepository;
import com.jira.auth.repository.UserRepository;
import com.jira.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SamlConfigService {

    private final SamlConfigurationRepository configRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public List<SamlConfiguration> listAll() {
        return configRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SamlConfiguration> listEnabled() {
        return configRepository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public SamlConfiguration getById(UUID id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SAML configuration not found: " + id));
    }

    @Transactional(readOnly = true)
    public SamlConfiguration getByRegistrationId(String registrationId) {
        return configRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new RuntimeException("SAML configuration not found: " + registrationId));
    }

    @Transactional
    public SamlConfiguration create(SamlConfiguration config) {
        if (configRepository.existsByRegistrationId(config.getRegistrationId())) {
            throw new IllegalArgumentException("Registration ID already exists: " + config.getRegistrationId());
        }
        return configRepository.save(config);
    }

    @Transactional
    public SamlConfiguration update(UUID id, SamlConfiguration update) {
        SamlConfiguration existing = getById(id);
        if (update.getName() != null) existing.setName(update.getName());
        if (update.getEntityId() != null) existing.setEntityId(update.getEntityId());
        if (update.getIdpEntityId() != null) existing.setIdpEntityId(update.getIdpEntityId());
        if (update.getIdpSsoUrl() != null) existing.setIdpSsoUrl(update.getIdpSsoUrl());
        if (update.getIdpSloUrl() != null) existing.setIdpSloUrl(update.getIdpSloUrl());
        if (update.getIdpCertificate() != null) existing.setIdpCertificate(update.getIdpCertificate());
        if (update.getSpEntityId() != null) existing.setSpEntityId(update.getSpEntityId());
        if (update.getAcsUrl() != null) existing.setAcsUrl(update.getAcsUrl());
        if (update.getAttributeMappingEmail() != null) existing.setAttributeMappingEmail(update.getAttributeMappingEmail());
        if (update.getAttributeMappingUsername() != null) existing.setAttributeMappingUsername(update.getAttributeMappingUsername());
        if (update.getAttributeMappingDisplayName() != null) existing.setAttributeMappingDisplayName(update.getAttributeMappingDisplayName());
        if (update.getAttributeMappingGroups() != null) existing.setAttributeMappingGroups(update.getAttributeMappingGroups());
        if (update.getDefaultRole() != null) existing.setDefaultRole(update.getDefaultRole());
        if (update.getAutoCreateUsers() != null) existing.setAutoCreateUsers(update.getAutoCreateUsers());
        if (update.getEnabled() != null) existing.setEnabled(update.getEnabled());
        if (update.getForceAuthn() != null) existing.setForceAuthn(update.getForceAuthn());
        if (update.getSingleLogoutEnabled() != null) existing.setSingleLogoutEnabled(update.getSingleLogoutEnabled());
        return configRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (!configRepository.existsById(id)) {
            throw new RuntimeException("SAML configuration not found: " + id);
        }
        configRepository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> authenticateSamlUser(String nameId, String registrationId,
                                                     Map<String, String> attributes) {
        log.info("SAML authentication for nameId={}, registrationId={}", nameId, registrationId);

        SamlConfiguration config = getByRegistrationId(registrationId);

        String email = attributes.getOrDefault(config.getAttributeMappingEmail(), nameId);
        String username = attributes.getOrDefault(config.getAttributeMappingUsername(), nameId);
        String displayName = attributes.getOrDefault(config.getAttributeMappingDisplayName(), username);

        Optional<User> existingUser = userRepository.findBySamlNameIdAndSamlIdpId(nameId, registrationId);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!user.getActive()) {
                throw new RuntimeException("User account is disabled");
            }
            log.info("SAML user found: {} ({})", user.getUsername(), user.getId());
        } else if (config.getAutoCreateUsers()) {
            user = provisionUser(nameId, registrationId, username, email, config.getDefaultRole());
            log.info("SAML user provisioned: {} ({})", user.getUsername(), user.getId());
        } else {
            throw new RuntimeException("User not found and auto-create is disabled for this IdP");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleKey)
                .collect(Collectors.toSet());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roleNames);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("roles", roleNames);
        response.put("authProvider", "SAML");
        return response;
    }

    private User provisionUser(String nameId, String idpId, String username, String email, String defaultRoleKey) {
        String uniqueUsername = username;
        int counter = 1;
        while (userRepository.existsByUsername(uniqueUsername)) {
            uniqueUsername = username + "_" + counter++;
        }

        String uniqueEmail = email;
        if (email == null || email.isBlank()) {
            uniqueEmail = uniqueUsername + "@saml.local";
        }
        counter = 1;
        while (userRepository.existsByEmail(uniqueEmail)) {
            uniqueEmail = username + "_" + counter++ + "@saml.local";
        }

        Role defaultRole = roleRepository.findByRoleKey(defaultRoleKey)
                .orElseGet(() -> roleRepository.findByRoleKey("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Default role not found")));

        User user = User.builder()
                .username(uniqueUsername)
                .email(uniqueEmail)
                .passwordHash(null)
                .authProvider("SAML")
                .samlNameId(nameId)
                .samlIdpId(idpId)
                .active(true)
                .roles(Set.of(defaultRole))
                .build();

        return userRepository.save(user);
    }
}
