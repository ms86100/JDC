package com.jira.migration.service;

import com.jira.migration.entity.OptionMapping;
import com.jira.migration.repository.OptionMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OptionMappingService {

    private final OptionMappingRepository optionMappingRepository;

    @Transactional
    public List<OptionMapping> saveForJob(UUID jobId, List<Map<String, Object>> mappings) {
        optionMappingRepository.deleteByJobId(jobId);
        return persist(jobId, null, mappings);
    }

    @Transactional
    public List<OptionMapping> saveForSession(UUID sessionId, List<Map<String, Object>> mappings) {
        optionMappingRepository.deleteByWizardSessionId(sessionId);
        return persist(null, sessionId, mappings);
    }

    @Transactional(readOnly = true)
    public List<OptionMapping> getForJob(UUID jobId) {
        return optionMappingRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public List<OptionMapping> getForSession(UUID sessionId) {
        return optionMappingRepository.findByWizardSessionId(sessionId);
    }

    public String resolveOptionValue(
            UUID jobId,
            String sourceFieldKey,
            String sourceValue,
            List<OptionMapping> cached) {
        if (sourceValue == null) {
            return null;
        }
        List<OptionMapping> mappings = cached != null ? cached : optionMappingRepository.findByJobId(jobId);
        return mappings.stream()
                .filter(m -> m.getSourceFieldKey().equalsIgnoreCase(sourceFieldKey)
                        && m.getSourceOptionValue().equalsIgnoreCase(sourceValue.trim()))
                .map(OptionMapping::getTargetOptionValue)
                .findFirst()
                .orElse(sourceValue);
    }

    private List<OptionMapping> persist(UUID jobId, UUID sessionId, List<Map<String, Object>> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            return List.of();
        }
        List<OptionMapping> entities = new ArrayList<>();
        for (Map<String, Object> m : mappings) {
            String sourceField = stringVal(m.get("sourceFieldKey"));
            String sourceOpt = stringVal(m.get("sourceOptionValue"));
            String targetField = stringVal(m.get("targetFieldKey"));
            String targetOpt = stringVal(m.get("targetOptionValue"));
            if (sourceField == null || sourceOpt == null || targetField == null || targetOpt == null) {
                continue;
            }
            entities.add(OptionMapping.builder()
                    .jobId(jobId)
                    .wizardSessionId(sessionId)
                    .sourceFieldKey(sourceField)
                    .sourceOptionValue(sourceOpt)
                    .targetFieldKey(targetField)
                    .targetOptionValue(targetOpt)
                    .build());
        }
        return optionMappingRepository.saveAll(entities);
    }

    private String stringVal(Object o) {
        return o == null ? null : o.toString().trim();
    }
}
