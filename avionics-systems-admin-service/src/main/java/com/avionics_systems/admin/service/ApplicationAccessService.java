package com.avionics_systems.admin.service;

import com.avionics_systems.admin.entity.ApplicationAccessEntity;
import com.avionics_systems.admin.entity.ApplicationAccessId;
import com.avionics_systems.admin.repository.ApplicationAccessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationAccessService {

    private final ApplicationAccessRepository applicationAccessRepository;

    @Transactional(readOnly = true)
    public List<ApplicationAccessEntity> getUserAccess(UUID userId) {
        return applicationAccessRepository.findByIdUserId(userId);
    }

    @Transactional
    public List<ApplicationAccessEntity> updateUserAccess(UUID userId, List<String> applicationKeys,
                                                           UUID grantedBy) {
        applicationAccessRepository.deleteByIdUserId(userId);

        List<ApplicationAccessEntity> grants = applicationKeys.stream()
                .map(key -> ApplicationAccessEntity.builder()
                        .id(ApplicationAccessId.builder()
                                .userId(userId)
                                .applicationKey(key)
                                .build())
                        .grantedAt(LocalDateTime.now())
                        .grantedBy(grantedBy)
                        .build())
                .collect(Collectors.toList());

        List<ApplicationAccessEntity> saved = applicationAccessRepository.saveAll(grants);
        log.info("Updated application access for user {}: {} applications", userId, saved.size());
        return saved;
    }

    @Transactional
    public ApplicationAccessEntity grantAccess(UUID userId, String applicationKey, UUID grantedBy) {
        ApplicationAccessId id = ApplicationAccessId.builder()
                .userId(userId)
                .applicationKey(applicationKey)
                .build();

        Optional<ApplicationAccessEntity> existing = applicationAccessRepository.findById(id);
        if (existing.isPresent()) {
            return existing.get();
        }

        ApplicationAccessEntity entity = ApplicationAccessEntity.builder()
                .id(id)
                .grantedAt(LocalDateTime.now())
                .grantedBy(grantedBy)
                .build();

        return applicationAccessRepository.save(entity);
    }

    @Transactional
    public boolean revokeAccess(UUID userId, String applicationKey) {
        ApplicationAccessId id = ApplicationAccessId.builder()
                .userId(userId)
                .applicationKey(applicationKey)
                .build();

        if (applicationAccessRepository.existsById(id)) {
            applicationAccessRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
