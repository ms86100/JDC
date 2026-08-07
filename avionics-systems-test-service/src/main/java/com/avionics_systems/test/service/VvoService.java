package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.HlvvoDefinition;
import com.avionics_systems.test.entity.TestRequest;
import com.avionics_systems.test.entity.VvoDefinition;
import com.avionics_systems.test.entity.VvoTestRequestLink;
import com.avionics_systems.test.exception.ResourceNotFoundException;
import com.avionics_systems.test.repository.HlvvoDefinitionRepository;
import com.avionics_systems.test.repository.TestRequestRepository;
import com.avionics_systems.test.repository.VvoDefinitionRepository;
import com.avionics_systems.test.repository.VvoTestRequestLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VvoService {

    private static final Pattern VVO_NAMING_PATTERN =
            Pattern.compile("\\[VVO_nFMS_\\w+_\\d+\\.\\d+].*");

    private final VvoDefinitionRepository vvoRepo;
    private final HlvvoDefinitionRepository hlvvoRepo;
    private final TestRequestRepository testRequestRepo;
    private final VvoTestRequestLinkRepository linkRepo;

    // ========== VVO CRUD ==========

    @Transactional
    public VvoResponse createVvo(CreateVvoRequest request) {
        log.info("Creating VVO for project: {}", request.getProjectId());

        // G3: Validate VVO naming convention (warn only, don't reject)
        validateVvoNaming(request.getSummary());

        long sequence = vvoRepo.countByProjectId(request.getProjectId()) + 1;
        String issueKey = "VVO-" + sequence;

        VvoDefinition entity = VvoDefinition.builder()
                .projectId(request.getProjectId())
                .issueKey(issueKey)
                .summary(request.getSummary())
                .description(request.getDescription())
                .status("NEW")
                .hlvvoId(request.getHlvvoId())
                .executionResponsible(request.getExecutionResponsible())
                .executionDelegation(request.getExecutionDelegation())
                .vvoUsage(request.getVvoUsage())
                .vvoScope(request.getVvoScope())
                .testMeanTypeRequested(request.getTestMeanTypeRequested())
                .operationalConditions(request.getOperationalConditions())
                .expectedResults(request.getExpectedResults())
                .realSystemNeeded(request.getRealSystemNeeded())
                .applicability(request.getApplicability())
                .supplierApplicability(request.getSupplierApplicability())
                .associatedRequirements(request.getAssociatedRequirements())
                .milestoneTarget(request.getMilestoneTarget())
                .specificationReference(request.getSpecificationReference())
                .assigneeId(request.getAssigneeId())
                .storyPoints(request.getStoryPoints())
                .labels(request.getLabels())
                .componentIds(request.getComponentIds())
                .ptsMfclLinks(request.getPtsMfclLinks())
                .nDi(request.getNDi())
                .referenceDocuments(request.getReferenceDocuments())
                .dtsBaselineVersion(request.getDtsBaselineVersion())
                .vvoVersion(1)
                .archived(false)
                .build();

        entity = vvoRepo.save(entity);
        log.info("VVO created with id: {} issueKey: {}", entity.getId(), entity.getIssueKey());
        return mapToVvoResponse(entity);
    }

    @Transactional(readOnly = true)
    public VvoResponse getVvo(UUID id) {
        VvoDefinition entity = vvoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VVO", "id", id));
        return mapToVvoResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<VvoResponse> getVvosByProject(UUID projectId) {
        return vvoRepo.findByProjectIdAndArchivedFalse(projectId).stream()
                .map(this::mapToVvoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VvoResponse> getVvosByHlvvo(UUID hlvvoId) {
        return vvoRepo.findByHlvvoId(hlvvoId).stream()
                .map(this::mapToVvoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VvoResponse getVvoByDoorsId(String idDoors) {
        VvoDefinition entity = vvoRepo.findByIdDoors(idDoors)
                .orElseThrow(() -> new ResourceNotFoundException("VVO", "idDoors", idDoors));
        return mapToVvoResponse(entity);
    }

    @Transactional
    public VvoResponse updateVvo(UUID id, UpdateVvoRequest request) {
        log.info("Updating VVO: {}", id);
        VvoDefinition entity = vvoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VVO", "id", id));

        if (request.getSummary() != null) {
            entity.setSummary(request.getSummary());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getHlvvoId() != null) {
            entity.setHlvvoId(request.getHlvvoId());
        }
        if (request.getExecutionResponsible() != null) {
            entity.setExecutionResponsible(request.getExecutionResponsible());
        }
        if (request.getExecutionDelegation() != null) {
            entity.setExecutionDelegation(request.getExecutionDelegation());
        }
        if (request.getVvoUsage() != null) {
            entity.setVvoUsage(request.getVvoUsage());
        }
        if (request.getVvoScope() != null) {
            entity.setVvoScope(request.getVvoScope());
        }
        if (request.getTestMeanTypeRequested() != null) {
            entity.setTestMeanTypeRequested(request.getTestMeanTypeRequested());
        }
        if (request.getOperationalConditions() != null) {
            entity.setOperationalConditions(request.getOperationalConditions());
        }
        if (request.getExpectedResults() != null) {
            entity.setExpectedResults(request.getExpectedResults());
        }
        if (request.getRealSystemNeeded() != null) {
            entity.setRealSystemNeeded(request.getRealSystemNeeded());
        }
        if (request.getApplicability() != null) {
            entity.setApplicability(request.getApplicability());
        }
        if (request.getSupplierApplicability() != null) {
            entity.setSupplierApplicability(request.getSupplierApplicability());
        }
        if (request.getAssociatedRequirements() != null) {
            entity.setAssociatedRequirements(request.getAssociatedRequirements());
        }
        if (request.getIdDoors() != null) {
            entity.setIdDoors(request.getIdDoors());
        }
        if (request.getMilestoneTarget() != null) {
            entity.setMilestoneTarget(request.getMilestoneTarget());
        }
        if (request.getSpecificationReference() != null) {
            entity.setSpecificationReference(request.getSpecificationReference());
        }
        if (request.getAssigneeId() != null) {
            entity.setAssigneeId(request.getAssigneeId());
        }
        if (request.getFixVersionId() != null) {
            entity.setFixVersionId(request.getFixVersionId());
        }
        if (request.getStoryPoints() != null) {
            entity.setStoryPoints(request.getStoryPoints());
        }
        if (request.getLabels() != null) {
            entity.setLabels(request.getLabels());
        }
        if (request.getComponentIds() != null) {
            entity.setComponentIds(request.getComponentIds());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        if (request.getPtsMfclLinks() != null) {
            entity.setPtsMfclLinks(request.getPtsMfclLinks());
        }
        if (request.getNDi() != null) {
            entity.setNDi(request.getNDi());
        }
        if (request.getReferenceDocuments() != null) {
            entity.setReferenceDocuments(request.getReferenceDocuments());
        }
        if (request.getDtsBaselineVersion() != null) {
            entity.setDtsBaselineVersion(request.getDtsBaselineVersion());
        }
        if (request.getBaselineVerified() != null) {
            entity.setBaselineVerified(request.getBaselineVerified());
        }

        entity = vvoRepo.save(entity);
        log.info("VVO updated: {}", id);
        return mapToVvoResponse(entity);
    }

    @Transactional
    public VvoCloneResponse cloneVvo(UUID id) {
        VvoDefinition original = vvoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VVO", "id", id));

        int newVersion = (original.getVvoVersion() != null ? original.getVvoVersion() : 1) + 1;
        log.info("Cloning VVO {} to version {}", id, newVersion);

        long sequence = vvoRepo.countByProjectId(original.getProjectId()) + 1;
        String issueKey = "VVO-" + sequence;

        VvoDefinition clone = VvoDefinition.builder()
                .projectId(original.getProjectId())
                .issueKey(issueKey)
                .summary(original.getSummary())
                .description(original.getDescription())
                .status("NEW")
                .hlvvoId(null)
                .executionResponsible(original.getExecutionResponsible())
                .executionDelegation(original.getExecutionDelegation())
                .vvoUsage(original.getVvoUsage())
                .vvoScope(original.getVvoScope())
                .testMeanTypeRequested(original.getTestMeanTypeRequested())
                .operationalConditions(original.getOperationalConditions())
                .expectedResults(original.getExpectedResults())
                .realSystemNeeded(original.getRealSystemNeeded())
                .applicability(original.getApplicability())
                .supplierApplicability(original.getSupplierApplicability())
                .associatedRequirements(original.getAssociatedRequirements())
                .idDoors(original.getIdDoors())
                .vvoVersion(newVersion)
                .cloneSourceId(original.getId())
                .fixVersionId(null)
                .milestoneTarget(original.getMilestoneTarget())
                .specificationReference(original.getSpecificationReference())
                .assigneeId(original.getAssigneeId())
                .storyPoints(original.getStoryPoints())
                .labels(original.getLabels())
                .componentIds(original.getComponentIds())
                .ptsMfclLinks(original.getPtsMfclLinks())
                .nDi(original.getNDi())
                .referenceDocuments(original.getReferenceDocuments())
                .dtsBaselineVersion(original.getDtsBaselineVersion())
                .archived(false)
                .createdBy(original.getCreatedBy())
                .build();

        clone = vvoRepo.save(clone);
        log.info("VVO cloned successfully: {} -> {}", id, clone.getId());

        return VvoCloneResponse.builder()
                .id(clone.getId())
                .issueKey(clone.getIssueKey())
                .vvoVersion(clone.getVvoVersion())
                .cloneSourceId(clone.getCloneSourceId())
                .status(clone.getStatus())
                .build();
    }

    @Transactional
    public void archiveVvo(UUID id) {
        log.info("Archiving VVO: {}", id);
        VvoDefinition entity = vvoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VVO", "id", id));
        entity.setArchived(true);
        vvoRepo.save(entity);
        log.info("VVO archived: {}", id);
    }

    @Transactional(readOnly = true)
    public List<VvoResponse> getVvosByFixVersion(UUID fixVersionId) {
        return vvoRepo.findByFixVersionId(fixVersionId).stream()
                .map(this::mapToVvoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VvoResponse> getVvosByStatuses(UUID projectId, List<String> statuses) {
        return vvoRepo.findByProjectIdAndStatusIn(projectId, statuses).stream()
                .map(this::mapToVvoResponse)
                .toList();
    }

    // ========== HLVVO CRUD ==========

    @Transactional
    public HlvvoResponse createHlvvo(CreateHlvvoRequest request) {
        log.info("Creating HLVVO for project: {}", request.getProjectId());

        long sequence = hlvvoRepo.countByProjectId(request.getProjectId()) + 1;
        String issueKey = "HLVVO-" + sequence;

        HlvvoDefinition entity = HlvvoDefinition.builder()
                .projectId(request.getProjectId())
                .issueKey(issueKey)
                .summary(request.getSummary())
                .description(request.getDescription())
                .status("NEW")
                .targetDate(request.getTargetDate())
                .airbusReference(request.getAirbusReference())
                .hlvvoVersion(1)
                .assigneeId(request.getAssigneeId())
                .specificationReference(request.getSpecificationReference())
                .componentIds(request.getComponentIds())
                .taskProgress(0)
                .ptsLink(request.getPtsLink())
                .mfclLink(request.getMfclLink())
                .fixVersionId(request.getFixVersionId())
                .labels(request.getLabels())
                .build();

        entity = hlvvoRepo.save(entity);
        log.info("HLVVO created with id: {} issueKey: {}", entity.getId(), entity.getIssueKey());
        return mapToHlvvoResponse(entity);
    }

    @Transactional(readOnly = true)
    public HlvvoResponse getHlvvo(UUID id) {
        HlvvoDefinition entity = hlvvoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HLVVO", "id", id));
        return mapToHlvvoResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<HlvvoResponse> getHlvvosByProject(UUID projectId) {
        return hlvvoRepo.findByProjectId(projectId).stream()
                .map(this::mapToHlvvoResponse)
                .toList();
    }

    @Transactional
    public HlvvoResponse updateHlvvo(UUID id, CreateHlvvoRequest request) {
        log.info("Updating HLVVO: {}", id);
        HlvvoDefinition entity = hlvvoRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HLVVO", "id", id));

        if (request.getSummary() != null) {
            entity.setSummary(request.getSummary());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getTargetDate() != null) {
            entity.setTargetDate(request.getTargetDate());
        }
        if (request.getAirbusReference() != null) {
            entity.setAirbusReference(request.getAirbusReference());
        }
        if (request.getAssigneeId() != null) {
            entity.setAssigneeId(request.getAssigneeId());
        }
        if (request.getSpecificationReference() != null) {
            entity.setSpecificationReference(request.getSpecificationReference());
        }
        if (request.getComponentIds() != null) {
            entity.setComponentIds(request.getComponentIds());
        }
        if (request.getPtsLink() != null) {
            entity.setPtsLink(request.getPtsLink());
        }
        if (request.getMfclLink() != null) {
            entity.setMfclLink(request.getMfclLink());
        }
        if (request.getFixVersionId() != null) {
            entity.setFixVersionId(request.getFixVersionId());
        }
        if (request.getLabels() != null) {
            entity.setLabels(request.getLabels());
        }

        entity = hlvvoRepo.save(entity);
        log.info("HLVVO updated: {}", id);
        return mapToHlvvoResponse(entity);
    }

    // ========== Test Request CRUD ==========

    @Transactional
    public TestRequestResponse createTestRequest(CreateTestRequestRequest request) {
        log.info("Creating Test Request for project: {}", request.getProjectId());

        long sequence = testRequestRepo.countByProjectId(request.getProjectId()) + 1;
        String requestType = request.getRequestType() != null ? request.getRequestType() : "LTR";
        String issueKey = requestType + "-" + sequence;

        TestRequest entity = TestRequest.builder()
                .projectId(request.getProjectId())
                .issueKey(issueKey)
                .summary(request.getSummary())
                .description(request.getDescription())
                .requestType(requestType)
                .status("NEW")
                .fixVersionId(request.getFixVersionId())
                .assigneeId(request.getAssigneeId())
                .frozen(false)
                .labels(request.getLabels())
                .build();

        entity = testRequestRepo.save(entity);
        log.info("Test Request created with id: {} issueKey: {}", entity.getId(), entity.getIssueKey());
        return mapToTestRequestResponse(entity);
    }

    @Transactional(readOnly = true)
    public TestRequestResponse getTestRequest(UUID id) {
        TestRequest entity = testRequestRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestRequest", "id", id));
        return mapToTestRequestResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<TestRequestResponse> getTestRequestsByProject(UUID projectId) {
        return testRequestRepo.findByProjectId(projectId).stream()
                .map(this::mapToTestRequestResponse)
                .toList();
    }

    // ========== VVO-TestRequest Links ==========

    @Transactional
    public void linkVvoToTestRequest(UUID vvoId, UUID testRequestId) {
        log.info("Linking VVO {} to Test Request {}", vvoId, testRequestId);

        // Validate both exist
        vvoRepo.findById(vvoId)
                .orElseThrow(() -> new ResourceNotFoundException("VVO", "id", vvoId));
        TestRequest testRequest = testRequestRepo.findById(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRequest", "id", testRequestId));

        // Enforce frozen status: when TestRequest is DONE (frozen=true), links cannot be modified
        enforceFrozenCheck(testRequest);

        if (linkRepo.existsByVvoIdAndTestRequestId(vvoId, testRequestId)) {
            log.info("Link already exists between VVO {} and Test Request {}", vvoId, testRequestId);
            return;
        }

        VvoTestRequestLink link = VvoTestRequestLink.builder()
                .vvoId(vvoId)
                .testRequestId(testRequestId)
                .linkType("CONTAIN")
                .build();
        linkRepo.save(link);
        log.info("Linked VVO {} to Test Request {}", vvoId, testRequestId);
    }

    @Transactional
    public void unlinkVvoFromTestRequest(UUID vvoId, UUID testRequestId) {
        log.info("Unlinking VVO {} from Test Request {}", vvoId, testRequestId);

        // Validate TestRequest exists and enforce frozen check
        TestRequest testRequest = testRequestRepo.findById(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRequest", "id", testRequestId));
        enforceFrozenCheck(testRequest);

        linkRepo.deleteByVvoIdAndTestRequestId(vvoId, testRequestId);
        log.info("Unlinked VVO {} from Test Request {}", vvoId, testRequestId);
    }

    /**
     * Enforce that when a TestRequest is in DONE status (frozen=true),
     * VVO links cannot be added or removed.
     */
    private void enforceFrozenCheck(TestRequest testRequest) {
        if (Boolean.TRUE.equals(testRequest.getFrozen()) || "DONE".equalsIgnoreCase(testRequest.getStatus())) {
            throw new IllegalStateException(
                    "TestRequest " + testRequest.getIssueKey() + " is frozen (status=DONE). "
                            + "VVO links cannot be added or removed.");
        }
    }

    @Transactional(readOnly = true)
    public List<VvoResponse> getVvosForTestRequest(UUID testRequestId) {
        return linkRepo.findByTestRequestId(testRequestId).stream()
                .map(link -> vvoRepo.findById(link.getVvoId()).orElse(null))
                .filter(Objects::nonNull)
                .map(this::mapToVvoResponse)
                .toList();
    }

    // ========== Private Mapping Methods ==========

    private VvoResponse mapToVvoResponse(VvoDefinition entity) {
        return VvoResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .issueKey(entity.getIssueKey())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .hlvvoId(entity.getHlvvoId())
                .executionResponsible(entity.getExecutionResponsible())
                .executionDelegation(entity.getExecutionDelegation())
                .vvoUsage(entity.getVvoUsage())
                .vvoScope(entity.getVvoScope())
                .testMeanTypeRequested(entity.getTestMeanTypeRequested())
                .operationalConditions(entity.getOperationalConditions())
                .expectedResults(entity.getExpectedResults())
                .realSystemNeeded(entity.getRealSystemNeeded())
                .applicability(entity.getApplicability())
                .supplierApplicability(entity.getSupplierApplicability())
                .associatedRequirements(entity.getAssociatedRequirements())
                .idDoors(entity.getIdDoors())
                .vvoVersion(entity.getVvoVersion())
                .cloneSourceId(entity.getCloneSourceId())
                .fixVersionId(entity.getFixVersionId())
                .milestoneTarget(entity.getMilestoneTarget())
                .specificationReference(entity.getSpecificationReference())
                .assigneeId(entity.getAssigneeId())
                .storyPoints(entity.getStoryPoints())
                .labels(entity.getLabels())
                .componentIds(entity.getComponentIds())
                .ptsMfclLinks(entity.getPtsMfclLinks())
                .nDi(entity.getNDi())
                .referenceDocuments(entity.getReferenceDocuments())
                .dtsBaselineVersion(entity.getDtsBaselineVersion())
                .baselineVerified(entity.getBaselineVerified())
                .archived(entity.getArchived())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private HlvvoResponse mapToHlvvoResponse(HlvvoDefinition entity) {
        return HlvvoResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .issueKey(entity.getIssueKey())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .targetDate(entity.getTargetDate())
                .airbusReference(entity.getAirbusReference())
                .hlvvoVersion(entity.getHlvvoVersion())
                .proofreadingData(entity.getProofreadingData())
                .assigneeId(entity.getAssigneeId())
                .specificationReference(entity.getSpecificationReference())
                .componentIds(entity.getComponentIds())
                .taskProgress(entity.getTaskProgress())
                .ptsLink(entity.getPtsLink())
                .mfclLink(entity.getMfclLink())
                .fixVersionId(entity.getFixVersionId())
                .labels(entity.getLabels())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TestRequestResponse mapToTestRequestResponse(TestRequest entity) {
        return TestRequestResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .issueKey(entity.getIssueKey())
                .summary(entity.getSummary())
                .description(entity.getDescription())
                .requestType(entity.getRequestType())
                .status(entity.getStatus())
                .fixVersionId(entity.getFixVersionId())
                .assigneeId(entity.getAssigneeId())
                .frozen(entity.getFrozen())
                .labels(entity.getLabels())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ========== G3: VVO Naming Validation ==========

    /**
     * Validates that the VVO summary follows the nFMS naming convention:
     * [VVO_nFMS_{function}_{version}] description
     * Logs a warning if the naming convention is not followed (does not reject).
     */
    private void validateVvoNaming(String summary) {
        if (summary == null || summary.isBlank()) {
            return;
        }
        if (!VVO_NAMING_PATTERN.matcher(summary).matches()) {
            log.warn("VVO summary does not follow nFMS naming convention " +
                    "[VVO_nFMS_<function>_<version>]: '{}'", summary);
        }
    }

    // ========== G7: Label Auto-Assignment Logic ==========

    /**
     * Suggests labels for a VVO based on associated requirements and supplier applicability,
     * following nFMS VVO Guidelines rules:
     * <ul>
     *   <li>If any requirement has change_rational containing "Change" -> add "Change" label</li>
     *   <li>If any requirement has "Merge" -> add "Merge" label</li>
     *   <li>If all requirements are only "Clarification" and/or "No change" -> add "Clarification" label</li>
     *   <li>If all requirements are only "No change" -> add "NoChange" label</li>
     *   <li>If supplier applicability indicates Thales-specific deviation -> add "Pureflyt" label</li>
     * </ul>
     *
     * @param associatedRequirements list of requirement change rationals (e.g., "Change", "Merge", "Clarification", "No change")
     * @param supplierApplicability  supplier applicability string (e.g., "THALES", "PUREFLYT")
     * @return list of suggested labels
     */
    public List<String> suggestLabels(List<String> associatedRequirements, String supplierApplicability) {
        List<String> labels = new ArrayList<>();

        if (associatedRequirements != null && !associatedRequirements.isEmpty()) {
            boolean hasChange = false;
            boolean hasMerge = false;
            boolean allNoChange = true;
            boolean allClarificationOrNoChange = true;

            for (String req : associatedRequirements) {
                if (req == null) continue;
                String lower = req.toLowerCase();

                if (lower.contains("change") && !lower.contains("no change")) {
                    hasChange = true;
                    allNoChange = false;
                    allClarificationOrNoChange = false;
                } else if (lower.contains("merge")) {
                    hasMerge = true;
                    allNoChange = false;
                    allClarificationOrNoChange = false;
                } else if (lower.contains("clarification")) {
                    allNoChange = false;
                } else if (lower.contains("no change")) {
                    // remains compatible with allNoChange and allClarificationOrNoChange
                } else {
                    // Unknown rational - breaks the "all" conditions
                    allNoChange = false;
                    allClarificationOrNoChange = false;
                }
            }

            if (hasChange) {
                labels.add("Change");
            }
            if (hasMerge) {
                labels.add("Merge");
            }
            if (allNoChange && !hasChange && !hasMerge) {
                labels.add("NoChange");
            } else if (allClarificationOrNoChange && !hasChange && !hasMerge) {
                labels.add("Clarification");
            }
        }

        // Thales-specific deviation
        if (supplierApplicability != null) {
            String lower = supplierApplicability.toLowerCase();
            if (lower.contains("thales") || lower.contains("pureflyt")) {
                labels.add("Pureflyt");
            }
        }

        return labels;
    }

    // ========== G8: DTS Baseline Tracking ==========

    /**
     * Flags all VVOs whose dts_baseline_version does not match the given version
     * by setting baseline_verified = false. This indicates those VVOs need re-verification
     * against the new DTS baseline.
     *
     * @param dtsVersion the new DTS baseline version to check against
     * @return the number of VVOs flagged for re-verification
     */
    @Transactional
    public int flagBaselineMismatch(String dtsVersion) {
        log.info("Flagging VVOs with DTS baseline version mismatch for version: {}", dtsVersion);
        int count = vvoRepo.flagBaselineNotMatching(dtsVersion);
        log.info("Flagged {} VVOs as baseline_verified=false for DTS version: {}", count, dtsVersion);
        return count;
    }
}
