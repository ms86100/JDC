package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.HlvvoDefinition;
import com.jira.test.entity.TestRequest;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.entity.VvoTestRequestLink;
import com.jira.test.exception.ResourceNotFoundException;
import com.jira.test.repository.HlvvoDefinitionRepository;
import com.jira.test.repository.TestRequestRepository;
import com.jira.test.repository.VvoDefinitionRepository;
import com.jira.test.repository.VvoTestRequestLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VvoService {

    private final VvoDefinitionRepository vvoRepo;
    private final HlvvoDefinitionRepository hlvvoRepo;
    private final TestRequestRepository testRequestRepo;
    private final VvoTestRequestLinkRepository linkRepo;

    // ========== VVO CRUD ==========

    @Transactional
    public VvoResponse createVvo(CreateVvoRequest request) {
        log.info("Creating VVO for project: {}", request.getProjectId());

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
        testRequestRepo.findById(testRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("TestRequest", "id", testRequestId));

        if (linkRepo.existsByVvoIdAndTestRequestId(vvoId, testRequestId)) {
            log.info("Link already exists between VVO {} and Test Request {}", vvoId, testRequestId);
            return;
        }

        VvoTestRequestLink link = VvoTestRequestLink.builder()
                .vvoId(vvoId)
                .testRequestId(testRequestId)
                .linkType("CONTAINS")
                .build();
        linkRepo.save(link);
        log.info("Linked VVO {} to Test Request {}", vvoId, testRequestId);
    }

    @Transactional
    public void unlinkVvoFromTestRequest(UUID vvoId, UUID testRequestId) {
        log.info("Unlinking VVO {} from Test Request {}", vvoId, testRequestId);
        linkRepo.deleteByVvoIdAndTestRequestId(vvoId, testRequestId);
        log.info("Unlinked VVO {} from Test Request {}", vvoId, testRequestId);
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
}
