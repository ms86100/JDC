package com.avionics_systems.project.controller;

import com.avionics_systems.project.dto.AddSecurityLevelMemberRequest;
import com.avionics_systems.project.dto.SecurityLevelMemberResponse;
import com.avionics_systems.project.entity.SecurityLevel;
import com.avionics_systems.project.entity.SecurityLevelMember;
import com.avionics_systems.project.repository.SecurityLevelMemberRepository;
import com.avionics_systems.project.repository.SecurityLevelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Security Level Member management.
 * Provides endpoints for managing members (users, groups, project roles) within security levels.
 */
@RestController
@RequestMapping("/api/security-levels")
@RequiredArgsConstructor
@Tag(name = "Security Level Members", description = "Security level member management endpoints")
public class SecurityLevelMemberController {

    private final SecurityLevelMemberRepository memberRepository;
    private final SecurityLevelRepository securityLevelRepository;

    @PostMapping("/{levelId}/members")
    @Operation(summary = "Add member to security level", description = "Adds a user, group, or project role to a security level")
    public ResponseEntity<?> addMember(
            @Parameter(description = "Security Level ID") @PathVariable UUID levelId,
            @Valid @RequestBody AddSecurityLevelMemberRequest request) {

        SecurityLevel securityLevel = securityLevelRepository.findById(levelId)
                .orElse(null);

        if (securityLevel == null) {
            return ResponseEntity.notFound().build();
        }

        UUID memberId = UUID.fromString(request.getMemberId());

        SecurityLevelMember member = SecurityLevelMember.builder()
                .securityLevel(securityLevel)
                .memberType(request.getMemberType())
                .memberId(memberId)
                .groupName(request.getGroupName())
                .addedAt(LocalDateTime.now())
                .build();

        SecurityLevelMember saved = memberRepository.save(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/{levelId}/members")
    @Operation(summary = "Get all members of a security level", description = "Returns all members (users, groups, project roles) of a security level")
    public ResponseEntity<List<SecurityLevelMemberResponse>> getMembers(
            @Parameter(description = "Security Level ID") @PathVariable UUID levelId) {

        List<SecurityLevelMemberResponse> members = memberRepository.findBySecurityLevelId(levelId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{levelId}/members/{memberId}")
    @Operation(summary = "Remove member from security level", description = "Removes a member from a security level by member ID")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Security Level ID") @PathVariable UUID levelId,
            @Parameter(description = "Member ID") @PathVariable UUID memberId) {

        List<SecurityLevelMember> members = memberRepository.findBySecurityLevelId(levelId);
        SecurityLevelMember toDelete = members.stream()
                .filter(m -> m.getMemberId().equals(memberId))
                .findFirst()
                .orElse(null);

        if (toDelete == null) {
            return ResponseEntity.notFound().build();
        }

        memberRepository.delete(toDelete);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{levelId}/members/bulk")
    @Operation(summary = "Bulk add members to security level", description = "Adds multiple members to a security level at once")
    public ResponseEntity<?> bulkAddMembers(
            @Parameter(description = "Security Level ID") @PathVariable UUID levelId,
            @Valid @RequestBody List<AddSecurityLevelMemberRequest> requests) {

        SecurityLevel securityLevel = securityLevelRepository.findById(levelId)
                .orElse(null);

        if (securityLevel == null) {
            return ResponseEntity.notFound().build();
        }

        List<SecurityLevelMember> members = requests.stream()
                .map(request -> SecurityLevelMember.builder()
                        .securityLevel(securityLevel)
                        .memberType(request.getMemberType())
                        .memberId(UUID.fromString(request.getMemberId()))
                        .groupName(request.getGroupName())
                        .addedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        List<SecurityLevelMember> saved = memberRepository.saveAll(members);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("count", saved.size(), "members", saved.stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList())));
    }

    private SecurityLevelMemberResponse toResponse(SecurityLevelMember entity) {
        return SecurityLevelMemberResponse.builder()
                .id(entity.getId())
                .securityLevelId(entity.getSecurityLevel().getId())
                .memberType(entity.getMemberType())
                .memberId(entity.getMemberId())
                .groupName(entity.getGroupName())
                .addedBy(entity.getAddedBy())
                .addedAt(entity.getAddedAt())
                .build();
    }
}