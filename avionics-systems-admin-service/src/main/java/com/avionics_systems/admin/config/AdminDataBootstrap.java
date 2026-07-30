package com.avionics_systems.admin.config;

import com.avionics_systems.admin.entity.IssueTypeSchemeEntity;
import com.avionics_systems.admin.repository.IssueTypeSchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a default issue type scheme when the admin DB has none (DC-style "Default" scheme).
 * Issue type membership is configured via Admin → Issue type schemes after types exist in issue-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataBootstrap implements ApplicationRunner {

    private final IssueTypeSchemeRepository issueTypeSchemeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (issueTypeSchemeRepository.count() > 0) {
            return;
        }
        IssueTypeSchemeEntity scheme = IssueTypeSchemeEntity.builder()
                .name("Default Issue Type Scheme")
                .description("System default — assign issue types via Configure in admin.")
                .issueTypeIds("")
                .projectCount(0)
                .isDefault(true)
                .build();
        issueTypeSchemeRepository.save(scheme);
        log.info("Seeded default issue type scheme: {}", scheme.getId());
    }
}
