package com.jira.user.service;

import com.jira.user.dto.*;
import com.jira.user.entity.Organization;
import com.jira.user.entity.OrganizationMember;
import com.jira.user.entity.Profile;
import com.jira.user.entity.Team;
import com.jira.user.exception.DuplicateResourceException;
import com.jira.user.exception.ResourceNotFoundException;
import com.jira.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final ProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Value("${app.defaults.timezone:UTC}")
    private String defaultTimezone;

    @Value("${app.defaults.member-role:MEMBER}")
    private String defaultMemberRole;

    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest request) {
        log.info("Creating profile for user: {}", request.getUserId());

        if (profileRepository.existsByUserId(request.getUserId())) {
            throw new DuplicateResourceException("Profile already exists for user: " + request.getUserId());
        }

        Profile profile = Profile.builder()
                .userId(request.getUserId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .avatarUrl(request.getAvatarUrl())
                .timezone(request.getTimezone() != null ? request.getTimezone() : defaultTimezone)
                .build();

        profile = profileRepository.save(profile);
        log.info("Created profile with id: {}", profile.getId());

        return mapToProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUserId(UUID userId) {
        log.debug("Fetching profile for user: {}", userId);

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        return mapToProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> getAllProfiles() {
        log.debug("Fetching all profiles");

        return profileRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, CreateProfileRequest request) {
        log.info("Updating profile for user: {}", userId);

        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone());
        }

        profile = profileRepository.save(profile);
        log.info("Updated profile with id: {}", profile.getId());

        return mapToProfileResponse(profile);
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        log.info("Creating organization with slug: {}", request.getSlug());

        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Organization with slug already exists: " + request.getSlug());
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .build();

        organization = organizationRepository.save(organization);
        log.info("Created organization with id: {}", organization.getId());

        return mapToOrganizationResponse(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        log.debug("Fetching organization with id: {}", id);

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        return mapToOrganizationResponse(organization);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        log.debug("Fetching all organizations");

        return organizationRepository.findAll().stream()
                .map(this::mapToOrganizationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizationMemberResponse addMemberToOrganization(UUID orgId, AddMemberRequest request) {
        log.info("Adding user {} to organization {}", request.getUserId(), orgId);

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        if (organizationMemberRepository.existsByOrgIdAndUserId(orgId, request.getUserId())) {
            throw new DuplicateResourceException("User already exists in organization");
        }

        OrganizationMember member = OrganizationMember.builder()
                .orgId(orgId)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : defaultMemberRole)
                .build();

        member = organizationMemberRepository.save(member);
        log.info("Added member to organization");

        Profile profile = profileRepository.findByUserId(request.getUserId()).orElse(null);
        String firstName = profile != null ? profile.getFirstName() : null;
        String lastName = profile != null ? profile.getLastName() : null;

        return OrganizationMemberResponse.builder()
                .orgId(orgId)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : defaultMemberRole)
                .userFirstName(firstName)
                .userLastName(lastName)
                .joinedAt(member.getJoinedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> getOrganizationMembers(UUID orgId) {
        log.debug("Fetching members for organization: {}", orgId);

        if (!organizationRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + orgId);
        }

        List<OrganizationMember> members = organizationMemberRepository.findByOrgId(orgId);

        return members.stream()
                .map(member -> {
                    Profile profile = profileRepository.findByUserId(member.getUserId()).orElse(null);
                    return OrganizationMemberResponse.builder()
                            .orgId(orgId)
                            .userId(member.getUserId())
                            .role(member.getRole())
                            .userFirstName(profile != null ? profile.getFirstName() : null)
                            .userLastName(profile != null ? profile.getLastName() : null)
                            .joinedAt(member.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        log.info("Creating team in organization: {}", request.getOrganizationId());

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + request.getOrganizationId()));

        Team team = Team.builder()
                .organization(organization)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        team = teamRepository.save(team);
        log.info("Created team with id: {}", team.getId());

        return mapToTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID id) {
        log.debug("Fetching team with id: {}", id);

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + id));

        return mapToTeamResponse(team);
    }

    private ProfileResponse mapToProfileResponse(Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .avatarUrl(profile.getAvatarUrl())
                .timezone(profile.getTimezone())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private OrganizationResponse mapToOrganizationResponse(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .slug(organization.getSlug())
                .createdAt(organization.getCreatedAt())
                .build();
    }

    private TeamResponse mapToTeamResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .organizationId(team.getOrganization().getId())
                .organizationName(team.getOrganization().getName())
                .name(team.getName())
                .description(team.getDescription())
                .createdAt(team.getCreatedAt())
                .build();
    }
}