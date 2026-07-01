package com.jira.portal.service;

import com.jira.portal.dto.*;
import com.jira.portal.entity.*;
import com.jira.portal.exception.ResourceNotFoundException;
import com.jira.portal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalService {

    private final CustomerPortalRepository customerPortalRepository;
    private final CustomerRequestRepository customerRequestRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final PortalCommentRepository portalCommentRepository;

    private static final AtomicInteger requestCounter = new AtomicInteger(0);

    // Portal Management
    @Transactional
    public CustomerPortalResponse createPortal(CreatePortalRequest request, UUID userId) {
        log.info("Creating portal '{}' for project {}", request.getName(), request.getProjectId());

        CustomerPortal portal = CustomerPortal.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .portalKey(request.getPortalKey())
                .baseUrl(request.getBaseUrl())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .requireAuthentication(request.getRequireAuthentication() != null ? request.getRequireAuthentication() : true)
                .allowAnonymousSubmissions(request.getAllowAnonymousSubmissions() != null ? request.getAllowAnonymousSubmissions() : false)
                .brandingConfig(request.getBrandingConfig())
                .layoutConfig(request.getLayoutConfig())
                .headerContent(request.getHeaderContent())
                .footerContent(request.getFooterContent())
                .homepageContent(request.getHomepageContent())
                .requestTypeIds(request.getRequestTypeIds())
                .customCss(request.getCustomCss())
                .googleAnalyticsKey(request.getGoogleAnalyticsKey())
                .build();

        portal = customerPortalRepository.save(portal);

        return toCustomerPortalResponse(portal);
    }

    @Transactional(readOnly = true)
    public CustomerPortalResponse getPortal(UUID portalId) {
        CustomerPortal portal = customerPortalRepository.findById(portalId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPortal", "id", portalId));
        return toCustomerPortalResponse(portal);
    }

    @Transactional(readOnly = true)
    public CustomerPortalResponse getPortalByKey(String portalKey) {
        CustomerPortal portal = customerPortalRepository.findByPortalKey(portalKey)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPortal", "portalKey", portalKey));
        return toCustomerPortalResponse(portal);
    }

    @Transactional(readOnly = true)
    public List<CustomerPortalResponse> getPortalsByProject(UUID projectId) {
        return customerPortalRepository.findByProjectId(projectId).stream()
                .map(this::toCustomerPortalResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CustomerPortalResponse publishPortal(UUID portalId, UUID userId) {
        log.info("Publishing portal {} by user {}", portalId, userId);
        CustomerPortal portal = customerPortalRepository.findById(portalId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPortal", "id", portalId));
        portal.setStatus("PUBLISHED");
        portal.setPublishedAt(LocalDateTime.now());
        portal.setPublishedBy(userId);
        portal = customerPortalRepository.save(portal);
        return toCustomerPortalResponse(portal);
    }

    @Transactional(readOnly = true)
    public List<CustomerPortalResponse> getPublicPortals() {
        return customerPortalRepository.findPublicPortals().stream()
                .map(this::toCustomerPortalResponse)
                .collect(Collectors.toList());
    }

    // Customer Request Management
    @Transactional
    public CustomerRequestResponse createCustomerRequest(CreateCustomerRequestDto request) {
        log.info("Creating customer request for portal {}", request.getPortalId());

        CustomerPortal portal = customerPortalRepository.findById(request.getPortalId())
                .orElseThrow(() -> new ResourceNotFoundException("CustomerPortal", "id", request.getPortalId()));

        String requestKey = generateRequestKey(portal.getPortalKey());

        CustomerRequest customerRequest = CustomerRequest.builder()
                .portalId(request.getPortalId())
                .requestTypeId(request.getRequestTypeId())
                .requestKey(requestKey)
                .summary(request.getSummary())
                .description(request.getDescription())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerId(request.getCustomerId())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .organizationId(request.getOrganizationId())
                .organizationName(request.getOrganizationName())
                .fields(request.getFields())
                .attachments(request.getAttachments())
                .channel(request.getChannel() != null ? request.getChannel() : "WEB")
                .build();

        customerRequest = customerRequestRepository.save(customerRequest);

        return toCustomerRequestResponse(customerRequest);
    }

    @Transactional(readOnly = true)
    public CustomerRequestResponse getCustomerRequest(UUID requestId) {
        CustomerRequest request = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerRequest", "id", requestId));
        return toCustomerRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public CustomerRequestResponse getCustomerRequestByKey(String requestKey) {
        CustomerRequest request = customerRequestRepository.findByRequestKey(requestKey)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerRequest", "requestKey", requestKey));
        return toCustomerRequestResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<CustomerRequestResponse> getRequestsByPortal(UUID portalId, Pageable pageable) {
        return customerRequestRepository.findByPortalId(portalId, pageable)
                .map(this::toCustomerRequestResponse);
    }

    @Transactional
    public CustomerRequestResponse updateRequestStatus(UUID requestId, String status, UUID agentId) {
        log.info("Updating request {} status to {} by agent {}", requestId, status, agentId);
        CustomerRequest request = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerRequest", "id", requestId));

        request.setStatus(status);
        request.setAssignedAgentId(agentId);

        if ("IN_PROGRESS".equals(status) && request.getFirstResponseAt() == null) {
            request.setFirstResponseAt(LocalDateTime.now());
        }
        if ("RESOLVED".equals(status)) {
            request.setResolvedAt(LocalDateTime.now());
        }
        if ("CLOSED".equals(status)) {
            request.setClosedAt(LocalDateTime.now());
        }

        request = customerRequestRepository.save(request);
        return toCustomerRequestResponse(request);
    }

    // Request Type Management
    @Transactional
    public RequestTypeResponse createRequestType(UUID portalId, CreateRequestTypeRequest request) {
        log.info("Creating request type '{}' for portal {}", request.getName(), portalId);

        RequestType requestType = RequestType.builder()
                .portalId(portalId)
                .name(request.getName())
                .description(request.getDescription())
                .issueType(request.getIssueType())
                .issueTypeId(request.getIssueTypeId())
                .projectId(request.getProjectId())
                .fieldsConfig(request.getFieldsConfig())
                .instructions(request.getInstructions())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .slaMinutes(request.getSlaMinutes() != null ? request.getSlaMinutes() : 480)
                .workflowId(request.getWorkflowId())
                .build();

        requestType = requestTypeRepository.save(requestType);

        return toRequestTypeResponse(requestType);
    }

    @Transactional(readOnly = true)
    public List<RequestTypeResponse> getRequestTypesByPortal(UUID portalId) {
        return requestTypeRepository.findByPortalIdAndIsEnabledTrueOrderByDisplayOrderAsc(portalId).stream()
                .map(this::toRequestTypeResponse)
                .collect(Collectors.toList());
    }

    // Comment Management
    @Transactional
    public PortalCommentResponse addComment(UUID requestId, CreateCommentRequest request) {
        log.info("Adding comment to request {}", requestId);

        PortalComment comment = PortalComment.builder()
                .requestId(requestId)
                .content(request.getContent())
                .authorId(request.getAuthorId())
                .authorName(request.getAuthorName())
                .authorEmail(request.getAuthorEmail())
                .authorType(request.getAuthorType() != null ? request.getAuthorType() : "CUSTOMER")
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .isInternal(request.getIsInternal() != null ? request.getIsInternal() : false)
                .attachments(request.getAttachments())
                .build();

        comment = portalCommentRepository.save(comment);

        return toPortalCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public List<PortalCommentResponse> getRequestComments(UUID requestId, boolean includeInternal) {
        List<PortalComment> comments;
        if (includeInternal) {
            comments = portalCommentRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
        } else {
            comments = portalCommentRepository.findByRequestIdAndIsPublicTrueOrderByCreatedAtAsc(requestId);
        }

        return comments.stream()
                .map(this::toPortalCommentResponse)
                .collect(Collectors.toList());
    }

    // Response Mappings
    private String generateRequestKey(String portalKey) {
        int counter = requestCounter.incrementAndGet();
        return portalKey + "-" + counter;
    }

    private CustomerPortalResponse toCustomerPortalResponse(CustomerPortal portal) {
        return CustomerPortalResponse.builder()
                .id(portal.getId())
                .name(portal.getName())
                .description(portal.getDescription())
                .projectId(portal.getProjectId())
                .portalKey(portal.getPortalKey())
                .baseUrl(portal.getBaseUrl())
                .status(portal.getStatus())
                .isPublic(portal.getIsPublic())
                .requireAuthentication(portal.getRequireAuthentication())
                .allowAnonymousSubmissions(portal.getAllowAnonymousSubmissions())
                .brandingConfig(portal.getBrandingConfig())
                .layoutConfig(portal.getLayoutConfig())
                .headerContent(portal.getHeaderContent())
                .footerContent(portal.getFooterContent())
                .homepageContent(portal.getHomepageContent())
                .requestTypeIds(portal.getRequestTypeIds())
                .customCss(portal.getCustomCss())
                .googleAnalyticsKey(portal.getGoogleAnalyticsKey())
                .createdAt(portal.getCreatedAt())
                .updatedAt(portal.getUpdatedAt())
                .publishedAt(portal.getPublishedAt())
                .publishedBy(portal.getPublishedBy())
                .build();
    }

    private CustomerRequestResponse toCustomerRequestResponse(CustomerRequest request) {
        return CustomerRequestResponse.builder()
                .id(request.getId())
                .portalId(request.getPortalId())
                .requestTypeId(request.getRequestTypeId())
                .issueId(request.getIssueId())
                .requestKey(request.getRequestKey())
                .summary(request.getSummary())
                .description(request.getDescription())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerId(request.getCustomerId())
                .status(request.getStatus())
                .priority(request.getPriority())
                .assignedAgentId(request.getAssignedAgentId())
                .organizationId(request.getOrganizationId())
                .organizationName(request.getOrganizationName())
                .fields(request.getFields())
                .attachments(request.getAttachments())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .firstResponseAt(request.getFirstResponseAt())
                .resolvedAt(request.getResolvedAt())
                .closedAt(request.getClosedAt())
                .slaBreached(request.getSlaBreached())
                .channel(request.getChannel())
                .satisfactionRating(request.getSatisfactionRating())
                .satisfactionComment(request.getSatisfactionComment())
                .build();
    }

    private RequestTypeResponse toRequestTypeResponse(RequestType requestType) {
        return RequestTypeResponse.builder()
                .id(requestType.getId())
                .portalId(requestType.getPortalId())
                .name(requestType.getName())
                .description(requestType.getDescription())
                .issueType(requestType.getIssueType())
                .issueTypeId(requestType.getIssueTypeId())
                .projectId(requestType.getProjectId())
                .fieldsConfig(requestType.getFieldsConfig())
                .instructions(requestType.getInstructions())
                .isEnabled(requestType.getIsEnabled())
                .isDefault(requestType.getIsDefault())
                .iconUrl(requestType.getIconUrl())
                .displayOrder(requestType.getDisplayOrder())
                .slaMinutes(requestType.getSlaMinutes())
                .build();
    }

    private PortalCommentResponse toPortalCommentResponse(PortalComment comment) {
        return PortalCommentResponse.builder()
                .id(comment.getId())
                .requestId(comment.getRequestId())
                .content(comment.getContent())
                .authorId(comment.getAuthorId())
                .authorName(comment.getAuthorName())
                .authorEmail(comment.getAuthorEmail())
                .authorType(comment.getAuthorType())
                .isPublic(comment.getIsPublic())
                .isInternal(comment.getIsInternal())
                .createdAt(comment.getCreatedAt())
                .attachments(comment.getAttachments())
                .build();
    }
}