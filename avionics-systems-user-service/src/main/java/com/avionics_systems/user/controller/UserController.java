package com.avionics_systems.user.controller;

import com.avionics_systems.user.dto.*;
import com.avionics_systems.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/profiles")
    public ResponseEntity<ProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        log.info("POST /api/users/profiles - Creating profile for user: {}", request.getUserId());
        ProfileResponse response = userService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/profiles")
    public ResponseEntity<List<ProfileResponse>> getAllProfiles() {
        log.info("GET /api/users/profiles - Fetching all profiles");
        List<ProfileResponse> response = userService.getAllProfiles();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profiles/{userId}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID userId) {
        log.info("GET /api/users/profiles/{} - Fetching profile", userId);
        ProfileResponse response = userService.getProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profiles/{userId}")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateProfileRequest request) {
        log.info("PUT /api/users/profiles/{} - Updating profile", userId);
        ProfileResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        log.info("POST /api/users/organizations - Creating organization: {}", request.getName());
        OrganizationResponse response = userService.createOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/organizations/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID id) {
        log.info("GET /api/users/organizations/{} - Fetching organization", id);
        OrganizationResponse response = userService.getOrganizationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationResponse>> getAllOrganizations() {
        log.info("GET /api/users/organizations - Fetching all organizations");
        List<OrganizationResponse> response = userService.getAllOrganizations();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/organizations/{orgId}/members")
    public ResponseEntity<OrganizationMemberResponse> addMemberToOrganization(
            @PathVariable UUID orgId,
            @Valid @RequestBody AddMemberRequest request) {
        log.info("POST /api/users/organizations/{}/members - Adding member: {}", orgId, request.getUserId());
        OrganizationMemberResponse response = userService.addMemberToOrganization(orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/organizations/{orgId}/members")
    public ResponseEntity<List<OrganizationMemberResponse>> getOrganizationMembers(@PathVariable UUID orgId) {
        log.info("GET /api/users/organizations/{}/members - Fetching members", orgId);
        List<OrganizationMemberResponse> response = userService.getOrganizationMembers(orgId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/teams")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        log.info("POST /api/users/teams - Creating team: {}", request.getName());
        TeamResponse response = userService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/teams/{id}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable UUID id) {
        log.info("GET /api/users/teams/{} - Fetching team", id);
        TeamResponse response = userService.getTeamById(id);
        return ResponseEntity.ok(response);
    }
}