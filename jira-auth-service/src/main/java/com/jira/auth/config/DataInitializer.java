package com.jira.auth.config;

import com.jira.auth.entity.Role;
import com.jira.auth.entity.User;
import com.jira.auth.repository.RoleRepository;
import com.jira.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    @Value("${app.defaults.role-admin:ROLE_ADMIN}")
    private String roleAdmin;

    @Value("${app.defaults.role-user:ROLE_USER}")
    private String roleUser;

    @Value("${app.admin.username:ms86100}")
    private String adminUsername;

    @Value("${app.admin.email:admin@test.local}")
    private String adminEmail;

    @Value("${app.defaults.auth-provider-local:LOCAL}")
    private String authProviderLocal;

    @Value("${app.defaults.role-description-prefix:System }")
    private String roleDescriptionPrefix;

    @Override
    @Transactional
    public void run(String... args) {
        ensureRoleExists(roleAdmin);
        ensureRoleExists(roleUser);

        String adminPassword = System.getenv("ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            adminPassword = UUID.randomUUID().toString();
        }
        createAdminUserIfNotExists(adminUsername, adminEmail, adminPassword);
    }

    private void ensureRoleExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder()
                    .roleKey(roleName)
                    .name(roleName)
                    .description(roleDescriptionPrefix + roleName.replace("ROLE_", ""))
                    .build();
            roleRepository.save(role);
            log.info("Created role: {}", roleName);
        }
    }

    private void createAdminUserIfNotExists(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            User existing = userRepository.findByUsername(username).orElse(null);
            if (existing != null) {
                Role adminRole = roleRepository.findByName(roleAdmin).orElse(null);
                Role userRole = roleRepository.findByName(roleUser).orElse(null);
                if (adminRole != null && !existing.getRoles().contains(adminRole)) {
                    existing.getRoles().add(adminRole);
                    if (userRole != null) existing.getRoles().remove(userRole);
                    userRepository.save(existing);
                    log.info("Updated {} to ADMIN role", username);
                }
            }
            log.info("User {} already exists, skipping creation", username);
            return;
        }

        Role adminRole = roleRepository.findByName(roleAdmin)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("init.error.role.not.found", new Object[]{roleAdmin}, Locale.ENGLISH)));
        Role userRole = roleRepository.findByName(roleUser)
                .orElseThrow(() -> new RuntimeException(
                        messageSource.getMessage("init.error.role.not.found", new Object[]{roleUser}, Locale.ENGLISH)));

        User admin = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .authProvider(authProviderLocal)
                .active(true)
                .roles(new java.util.HashSet<>(Set.of(adminRole, userRole)))
                .build();

        userRepository.save(admin);
        log.info("Created admin user: {} (ADMIN role)", username);
    }
}
