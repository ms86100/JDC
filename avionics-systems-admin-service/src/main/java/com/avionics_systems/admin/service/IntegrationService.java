package com.avionics_systems.admin.service;

import com.avionics_systems.admin.dto.ApplicationLinkResponse;
import com.avionics_systems.admin.entity.ApplicationLinkEntity;
import com.avionics_systems.admin.repository.ApplicationLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final ApplicationLinkRepository applicationLinkRepository;
    private final MessageSource messageSource;

    @Value("${app.defaults.application-link-name:Confluence}")
    private String defaultApplicationLinkName;

    @Value("${app.defaults.application-type:confluence}")
    private String defaultApplicationType;

    @Value("${app.defaults.link-direction:two-way}")
    private String defaultLinkDirection;

    @Value("${app.defaults.link-initial-status:pending}")
    private String defaultLinkStatus;

    @Transactional(readOnly = true)
    public List<ApplicationLinkResponse> listApplicationLinks() {
        return applicationLinkRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApplicationLinkResponse createApplicationLink(Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", defaultApplicationLinkName));
        String url = String.valueOf(body.getOrDefault("url", "")).trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.integration.url.required", null, Locale.ENGLISH));
        }

        String applicationType = String.valueOf(body.getOrDefault("applicationType", defaultApplicationType));
        String direction = String.valueOf(body.getOrDefault("direction", defaultLinkDirection));

        boolean makePrimary = Boolean.TRUE.equals(body.get("primary"))
                || applicationLinkRepository.count() == 0;

        if (makePrimary) {
            applicationLinkRepository.findAll().forEach(link -> {
                link.setPrimary(false);
                applicationLinkRepository.save(link);
            });
        }

        ApplicationLinkEntity entity = ApplicationLinkEntity.builder()
                .name(name)
                .url(url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                .applicationType(applicationType)
                .direction(direction)
                .status(defaultLinkStatus)
                .primary(makePrimary)
                .build();

        return toResponse(applicationLinkRepository.save(entity));
    }

    @Transactional
    public void deleteApplicationLink(String id) {
        ApplicationLinkEntity link = applicationLinkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.integration.link.not.found", null, Locale.ENGLISH)));
        boolean wasPrimary = Boolean.TRUE.equals(link.getPrimary());
        applicationLinkRepository.delete(link);
        if (wasPrimary) {
            applicationLinkRepository.findAllByOrderByCreatedAtDesc().stream().findFirst()
                    .ifPresent(next -> {
                        next.setPrimary(true);
                        applicationLinkRepository.save(next);
                    });
        }
    }

    @Transactional
    public ApplicationLinkResponse setPrimary(String id) {
        ApplicationLinkEntity target = applicationLinkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.integration.link.not.found", null, Locale.ENGLISH)));
        applicationLinkRepository.findAll().forEach(link -> {
            link.setPrimary(link.getId().equals(target.getId()));
            applicationLinkRepository.save(link);
        });
        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public Map<String, String> testConnection(String id) {
        ApplicationLinkEntity link = applicationLinkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.integration.link.not.found", null, Locale.ENGLISH)));
        return Map.of(
                "linkId", link.getId(),
                "status", "pending",
                "message", "Link saved. OAuth 2.0 handshake and live health check are planned for integration-service."
        );
    }

    private ApplicationLinkResponse toResponse(ApplicationLinkEntity entity) {
        return ApplicationLinkResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .url(entity.getUrl())
                .applicationType(entity.getApplicationType())
                .direction(entity.getDirection())
                .status(entity.getStatus())
                .primary(Boolean.TRUE.equals(entity.getPrimary()))
                .build();
    }
}
