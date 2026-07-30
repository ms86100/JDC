package com.avionics_systems.plan.controller;

import com.avionics_systems.plan.dto.request.AddTeamMemberRequest;
import com.avionics_systems.plan.dto.request.CreateTeamRequest;
import com.avionics_systems.plan.dto.response.TeamMemberResponse;
import com.avionics_systems.plan.dto.response.TeamResponse;
import com.avionics_systems.plan.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getTeams(@PathVariable UUID planId) {
        return ResponseEntity.ok(teamService.getTeamsByPlanId(planId));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(
            @PathVariable UUID planId,
            @PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamById(planId, teamId));
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @PathVariable UUID planId,
            @Valid @RequestBody CreateTeamRequest request) {
        TeamResponse response = teamService.createTeam(planId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable UUID planId,
            @PathVariable UUID teamId,
            @RequestBody CreateTeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(planId, teamId, request));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable UUID planId,
            @PathVariable UUID teamId) {
        teamService.deleteTeam(planId, teamId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> addTeamMember(
            @PathVariable UUID planId,
            @PathVariable UUID teamId,
            @Valid @RequestBody AddTeamMemberRequest request) {
        TeamMemberResponse response = teamService.addTeamMember(teamId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> removeTeamMember(
            @PathVariable UUID planId,
            @PathVariable UUID teamId,
            @PathVariable UUID memberId) {
        teamService.removeTeamMember(teamId, memberId);
        return ResponseEntity.noContent().build();
    }
}
