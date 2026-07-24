package com.jira.test.service;

import com.jira.test.dto.VvoTransferResponse;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VvoTransferService {

    private final VvoDefinitionRepository vvoRepo;

    /**
     * Transfer VVOs from a Design Office (DO) project to a Lab (LAB) project.
     * Uses the ID Doors field as the unique cross-project identifier.
     *
     * For each RELEASED or CANCELLED VVO in the source baseline:
     *   - If a VVO with the same ID Doors already exists in the target project, update it
     *   - Otherwise, create a new VVO in the target project
     *
     * Set previewOnly=true to get a dry-run report without making changes.
     */
    @Transactional
    public VvoTransferResponse transferVvos(UUID sourceProjectId, UUID targetProjectId,
                                            UUID fixVersionId, boolean previewOnly) {
        List<VvoDefinition> sourceVvos = vvoRepo.findByFixVersionId(fixVersionId).stream()
                .filter(v -> v.getProjectId().equals(sourceProjectId))
                .filter(v -> List.of("RELEASED", "CANCELLED").contains(v.getStatus()))
                .filter(v -> v.getIdDoors() != null && !v.getIdDoors().isEmpty())
                .toList();

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> details = new ArrayList<>();

        for (VvoDefinition source : sourceVvos) {
            // Check if a VVO with matching ID Doors already exists in the target project
            Optional<VvoDefinition> existing = vvoRepo.findByIdDoors(source.getIdDoors());
            Optional<VvoDefinition> targetVvo = existing
                    .filter(v -> v.getProjectId().equals(targetProjectId));

            if (previewOnly) {
                if (targetVvo.isPresent()) {
                    details.add("UPDATE: " + source.getIssueKey() + " -> "
                            + targetVvo.get().getIssueKey());
                    updated++;
                } else {
                    details.add("CREATE: " + source.getIssueKey()
                            + " (ID Doors: " + source.getIdDoors() + ")");
                    created++;
                }
                continue;
            }

            if (targetVvo.isPresent()) {
                // Update existing VVO in target project
                VvoDefinition target = targetVvo.get();
                target.setSummary(source.getSummary());
                target.setDescription(source.getDescription());
                target.setExecutionResponsible(source.getExecutionResponsible());
                target.setVvoUsage(source.getVvoUsage());
                target.setVvoScope(source.getVvoScope());
                target.setTestMeanTypeRequested(source.getTestMeanTypeRequested());
                target.setOperationalConditions(source.getOperationalConditions());
                target.setExpectedResults(source.getExpectedResults());
                target.setRealSystemNeeded(source.getRealSystemNeeded());
                target.setApplicability(source.getApplicability());
                target.setSupplierApplicability(source.getSupplierApplicability());
                target.setVvoVersion(source.getVvoVersion());
                target.setSpecificationReference(source.getSpecificationReference());
                target.setStatus("UPDATE");
                vvoRepo.save(target);
                updated++;
            } else {
                // Create new VVO in target project as a read-only copy
                long seq = vvoRepo.countByProjectId(targetProjectId) + created + 1;
                VvoDefinition target = VvoDefinition.builder()
                        .projectId(targetProjectId)
                        .issueKey("VVO-" + seq)
                        .summary(source.getSummary())
                        .description(source.getDescription())
                        .status("NEW")
                        .executionResponsible(source.getExecutionResponsible())
                        .executionDelegation(source.getExecutionDelegation())
                        .vvoUsage(source.getVvoUsage())
                        .vvoScope(source.getVvoScope())
                        .testMeanTypeRequested(source.getTestMeanTypeRequested())
                        .operationalConditions(source.getOperationalConditions())
                        .expectedResults(source.getExpectedResults())
                        .realSystemNeeded(source.getRealSystemNeeded())
                        .applicability(source.getApplicability())
                        .supplierApplicability(source.getSupplierApplicability())
                        .associatedRequirements(source.getAssociatedRequirements())
                        .idDoors(source.getIdDoors())
                        .vvoVersion(source.getVvoVersion())
                        .fixVersionId(fixVersionId)
                        .specificationReference(source.getSpecificationReference())
                        .labels(source.getLabels())
                        .componentIds(source.getComponentIds())
                        .archived(false)
                        .build();
                vvoRepo.save(target);
                created++;
            }
        }

        log.info("VVO transfer from {} to {}: {} created, {} updated, {} skipped",
                sourceProjectId, targetProjectId, created, updated, skipped);

        return VvoTransferResponse.builder()
                .sourceProjectId(sourceProjectId)
                .targetProjectId(targetProjectId)
                .fixVersionId(fixVersionId)
                .createdCount(created)
                .updatedCount(updated)
                .skippedCount(skipped)
                .details(details)
                .previewOnly(previewOnly)
                .build();
    }
}
