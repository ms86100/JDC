package com.jira.auth.config;

import com.jira.auth.entity.Role;
import com.jira.auth.entity.User;
import com.jira.auth.repository.RoleRepository;
import com.jira.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        ensureRoleExists("ROLE_ADMIN");
        ensureRoleExists("ROLE_USER");

        createAdminUserIfNotExists("ms86100", "admin@test.local", "admin123");
    }

    private void ensureRoleExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder()
                    .name(roleName)
                    .description("System " + roleName.replace("ROLE_", ""))
                    .build();
            roleRepository.save(role);
            log.info("Created role: {}", roleName);
        }
    }

    private void createAdminUserIfNotExists(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            User existing = userRepository.findByUsername(username).orElse(null);
            if (existing != null) {
                Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElse(null);
                Role userRole = roleRepository.findByName("ROLE_USER").orElse(null);
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

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        User admin = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .active(true)
                .roles(new java.util.HashSet<>(Set.of(adminRole, userRole)))
                .build();

        userRepository.save(admin);
        log.info("Created admin user: {} / {} (ADMIN role)", username, password);
    }
}