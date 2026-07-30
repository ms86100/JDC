package com.avionics_systems.auth.service;

import com.avionics_systems.auth.entity.Role;
import com.avionics_systems.auth.entity.SamlConfiguration;
import com.avionics_systems.auth.entity.User;
import com.avionics_systems.auth.repository.RoleRepository;
import com.avionics_systems.auth.repository.SamlConfigurationRepository;
import com.avionics_systems.auth.repository.UserRepository;
import com.avionics_systems.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
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
    private final MessageSource messageSource;

    @Value("${app.defaults.role-user:ROLE_USER}")
    private String defaultUserRole;

    @Value("${app.defaults.auth-provider-saml:SAML}")
    private String authProviderSaml;

    @Value("${app.defaults.saml-email-domain:@saml.local}")
    private String samlEmailDomain;

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
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("saml.error.config.not.found", new Object[]{id}, Locale.ENGLISH)));
    }

    @Transactional(readOnly = true)
    public SamlConfiguration getByRegistrationId(String registrationId) {
        return configRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("saml.error.config.not.found", new Object[]{registrationId}, Locale.ENGLISH)));
    }

    @Transactional
    public SamlConfiguration create(SamlConfiguration config) {
        if (configRepository.existsByRegistrationId(config.getRegistrationId())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("saml.error.registration.exists", new Object[]{config.getRegistrationId()}, Locale.ENGLISH));
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
            throw new RuntimeException(
                    messageSource.getMessage("saml.error.config.not.found", new Object[]{id}, Locale.ENGLISH));
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
                throw new RuntimeException(
                        messageSource.getMessage("saml.error.user.disabled", null, Locale.ENGLISH));
            }
            log.info("SAML user found: {} ({})", user.getUsername(), user.getId());
        } else if (config.getAutoCreateUsers()) {
            user = provisionUser(nameId, registrationId, username, email, config.getDefaultRole());
            log.info("SAML user provisioned: {} ({})", user.getUsername(), user.getId());
        } else {
            throw new RuntimeException(
                    messageSource.getMessage("saml.error.auto.create.disabled", null, Locale.ENGLISH));
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
        response.put("authProvider", authProviderSaml);
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
            uniqueEmail = uniqueUsername + samlEmailDomain;
        }
        counter = 1;
        while (userRepository.existsByEmail(uniqueEmail)) {
            uniqueEmail = username + "_" + counter++ + samlEmailDomain;
        }

        Role defaultRole = roleRepository.findByRoleKey(defaultRoleKey)
                .orElseGet(() -> roleRepository.findByRoleKey(defaultUserRole)
                        .orElseThrow(() -> new RuntimeException(
                                messageSource.getMessage("saml.error.default.role.not.found", null, Locale.ENGLISH))));

        User user = User.builder()
                .username(uniqueUsername)
                .email(uniqueEmail)
                .passwordHash(null)
                .authProvider(authProviderSaml)
                .samlNameId(nameId)
                .samlIdpId(idpId)
                .active(true)
                .roles(Set.of(defaultRole))
                .build();

        return userRepository.save(user);
    }
}
