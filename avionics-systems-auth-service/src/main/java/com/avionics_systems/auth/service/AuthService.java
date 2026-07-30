package com.avionics_systems.auth.service;

import com.avionics_systems.auth.dto.*;
import com.avionics_systems.auth.entity.Role;
import com.avionics_systems.auth.entity.User;
import com.avionics_systems.auth.exception.AuthException;
import com.avionics_systems.auth.repository.RoleRepository;
import com.avionics_systems.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.avionics_systems.auth.security.JwtTokenProvider tokenProvider;
    private final MessageSource messageSource;

    @Value("${app.defaults.role-user:ROLE_USER}")
    private String defaultUserRole;

    @Value("${app.defaults.token-type:Bearer}")
    private String tokenType;

    @Value("${app.defaults.auth-provider-local:LOCAL}")
    private String authProviderLocal;

    @Value("${app.defaults.refresh-token-type:refresh}")
    private String refreshTokenType;

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException(messageSource.getMessage("auth.error.username.exists", null, Locale.ENGLISH));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException(messageSource.getMessage("auth.error.email.exists", null, Locale.ENGLISH));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(authProviderLocal)
                .active(true)
                .build();

        Role userRole = roleRepository.findByName(defaultUserRole)
                .orElseThrow(() -> new AuthException(
                        messageSource.getMessage("auth.error.role.not.found", new Object[]{defaultUserRole}, Locale.ENGLISH)));
        user.getRoles().add(userRole);

        user = userRepository.save(user);

        return toDto(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthException(
                        messageSource.getMessage("auth.error.invalid.credentials", null, Locale.ENGLISH)));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException(messageSource.getMessage("auth.error.invalid.credentials", null, Locale.ENGLISH));
        }

        if (!user.getActive()) {
            throw new AuthException(messageSource.getMessage("auth.error.account.disabled", null, Locale.ENGLISH));
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthException(messageSource.getMessage("auth.error.invalid.refresh.token", null, Locale.ENGLISH));
        }

        String tokenTypeValue = tokenProvider.getTokenType(refreshToken);
        if (!refreshTokenType.equals(tokenTypeValue)) {
            throw new AuthException(messageSource.getMessage("auth.error.not.refresh.token", null, Locale.ENGLISH));
        }

        UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        messageSource.getMessage("auth.error.user.not.found", null, Locale.ENGLISH)));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());

        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType(tokenType)
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        messageSource.getMessage("auth.error.user.not.found", null, Locale.ENGLISH)));
        return toDto(user);
    }

    private UserDto toDto(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());

        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .active(user.getActive())
                .roles(roles)
                .build();
    }
}
