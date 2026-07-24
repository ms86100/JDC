package com.jira.admin.service;

import com.jira.admin.dto.masterdata.*;
import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDataService {

    private final AircraftProgramRepository programRepo;
    private final TestMeanRepository testMeanRepo;
    private final AircraftSystemRepository systemRepo;
    private final AtaChapterRepository ataRepo;
    private final SystemSupplierRepository supplierRepo;
    private final SystemFunctionRepository functionRepo;
    private final ReporterTeamRepository teamRepo;
    private final TestMeanDefectOriginRepository defectOriginRepo;

    // ==================== Aircraft Programs ====================

    @Transactional(readOnly = true)
    public List<AircraftProgramResponse> getAllPrograms() {
        return programRepo.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(this::mapProgramToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AircraftProgramResponse createProgram(AircraftProgramRequest request) {
        AircraftProgramEntity entity = AircraftProgramEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .parentProgramId(request.getParentProgramId())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        AircraftProgramEntity saved = programRepo.save(entity);
        log.info("Created aircraft program: {} ({})", saved.getName(), saved.getId());
        return mapProgramToResponse(saved);
    }

    @Transactional
    public AircraftProgramResponse updateProgram(String id, AircraftProgramRequest request) {
        AircraftProgramEntity entity = programRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Aircraft program not found: " + id));
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setParentProgramId(request.getParentProgramId());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        AircraftProgramEntity saved = programRepo.save(entity);
        log.info("Updated aircraft program: {} ({})", saved.getName(), saved.getId());
        return mapProgramToResponse(saved);
    }

    @Transactional
    public void deactivateProgram(String id) {
        AircraftProgramEntity entity = programRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Aircraft program not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        programRepo.save(entity);
        log.info("Deactivated aircraft program: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== Test Means ====================

    @Transactional(readOnly = true)
    public List<TestMeanResponse> getTestMeansByProgram(String programId) {
        return testMeanRepo.findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(programId).stream()
                .map(this::mapTestMeanToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestMeanResponse createTestMean(TestMeanRequest request) {
        TestMeanEntity entity = TestMeanEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .programId(request.getProgramId())
                .category(request.getCategory())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        TestMeanEntity saved = testMeanRepo.save(entity);
        log.info("Created test mean: {} ({}) for program {}", saved.getName(), saved.getId(), saved.getProgramId());
        return mapTestMeanToResponse(saved);
    }

    @Transactional
    public TestMeanResponse updateTestMean(String id, TestMeanRequest request) {
        TestMeanEntity entity = testMeanRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Test mean not found: " + id));
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setProgramId(request.getProgramId());
        entity.setCategory(request.getCategory());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        TestMeanEntity saved = testMeanRepo.save(entity);
        log.info("Updated test mean: {} ({})", saved.getName(), saved.getId());
        return mapTestMeanToResponse(saved);
    }

    @Transactional
    public void deactivateTestMean(String id) {
        TestMeanEntity entity = testMeanRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Test mean not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        testMeanRepo.save(entity);
        log.info("Deactivated test mean: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== Aircraft Systems ====================

    @Transactional(readOnly = true)
    public List<AircraftSystemResponse> getSystemsByProgram(String programId) {
        return systemRepo.findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(programId).stream()
                .map(this::mapSystemToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AircraftSystemResponse createSystem(AircraftSystemRequest request) {
        AircraftSystemEntity entity = AircraftSystemEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .programId(request.getProgramId())
                .ataChapterCode(request.getAtaChapterCode())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        AircraftSystemEntity saved = systemRepo.save(entity);
        log.info("Created aircraft system: {} ({}) for program {}", saved.getName(), saved.getId(), saved.getProgramId());
        return mapSystemToResponse(saved);
    }

    @Transactional
    public AircraftSystemResponse updateSystem(String id, AircraftSystemRequest request) {
        AircraftSystemEntity entity = systemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Aircraft system not found: " + id));
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setProgramId(request.getProgramId());
        entity.setAtaChapterCode(request.getAtaChapterCode());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        AircraftSystemEntity saved = systemRepo.save(entity);
        log.info("Updated aircraft system: {} ({})", saved.getName(), saved.getId());
        return mapSystemToResponse(saved);
    }

    @Transactional
    public void deactivateSystem(String id) {
        AircraftSystemEntity entity = systemRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Aircraft system not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        systemRepo.save(entity);
        log.info("Deactivated aircraft system: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== ATA Chapters ====================

    @Transactional(readOnly = true)
    public List<AtaChapterResponse> getAtaChaptersByProgram(String programId) {
        return ataRepo.findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(programId).stream()
                .map(this::mapAtaChapterToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AtaChapterResponse createAtaChapter(AtaChapterRequest request) {
        AtaChapterEntity entity = AtaChapterEntity.builder()
                .id(UUID.randomUUID().toString())
                .chapterNumber(request.getChapterNumber())
                .title(request.getTitle())
                .programId(request.getProgramId())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        AtaChapterEntity saved = ataRepo.save(entity);
        log.info("Created ATA chapter: {} - {} ({}) for program {}", saved.getChapterNumber(), saved.getTitle(), saved.getId(), saved.getProgramId());
        return mapAtaChapterToResponse(saved);
    }

    @Transactional
    public AtaChapterResponse updateAtaChapter(String id, AtaChapterRequest request) {
        AtaChapterEntity entity = ataRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ATA chapter not found: " + id));
        entity.setChapterNumber(request.getChapterNumber());
        entity.setTitle(request.getTitle());
        entity.setProgramId(request.getProgramId());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        AtaChapterEntity saved = ataRepo.save(entity);
        log.info("Updated ATA chapter: {} - {} ({})", saved.getChapterNumber(), saved.getTitle(), saved.getId());
        return mapAtaChapterToResponse(saved);
    }

    @Transactional
    public void deactivateAtaChapter(String id) {
        AtaChapterEntity entity = ataRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ATA chapter not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        ataRepo.save(entity);
        log.info("Deactivated ATA chapter: {} - {} ({})", entity.getChapterNumber(), entity.getTitle(), entity.getId());
    }

    // ==================== System Suppliers ====================

    @Transactional(readOnly = true)
    public List<SystemSupplierResponse> getSuppliersByProgramAndSystem(String programId, String systemId) {
        return supplierRepo.findByProgramIdAndSystemIdAndIsActiveTrueOrderByDisplayOrderAsc(programId, systemId).stream()
                .map(this::mapSupplierToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SystemSupplierResponse> getSuppliersByProgram(String programId) {
        return supplierRepo.findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(programId).stream()
                .map(this::mapSupplierToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SystemSupplierResponse createSupplier(SystemSupplierRequest request) {
        SystemSupplierEntity entity = SystemSupplierEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(request.getCode())
                .name(request.getName())
                .programId(request.getProgramId())
                .systemId(request.getSystemId())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        SystemSupplierEntity saved = supplierRepo.save(entity);
        log.info("Created system supplier: {} ({}) for program {} / system {}", saved.getName(), saved.getId(), saved.getProgramId(), saved.getSystemId());
        return mapSupplierToResponse(saved);
    }

    @Transactional
    public SystemSupplierResponse updateSupplier(String id, SystemSupplierRequest request) {
        SystemSupplierEntity entity = supplierRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("System supplier not found: " + id));
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setProgramId(request.getProgramId());
        entity.setSystemId(request.getSystemId());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        SystemSupplierEntity saved = supplierRepo.save(entity);
        log.info("Updated system supplier: {} ({})", saved.getName(), saved.getId());
        return mapSupplierToResponse(saved);
    }

    @Transactional
    public void deactivateSupplier(String id) {
        SystemSupplierEntity entity = supplierRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("System supplier not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        supplierRepo.save(entity);
        log.info("Deactivated system supplier: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== System Functions ====================

    @Transactional(readOnly = true)
    public List<SystemFunctionResponse> getFunctionsBySystem(String systemId) {
        return functionRepo.findBySystemIdAndIsActiveTrueOrderByDisplayOrderAsc(systemId).stream()
                .map(this::mapFunctionToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SystemFunctionResponse createFunction(SystemFunctionRequest request) {
        SystemFunctionEntity entity = SystemFunctionEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .systemId(request.getSystemId())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        SystemFunctionEntity saved = functionRepo.save(entity);
        log.info("Created system function: {} ({}) for system {}", saved.getName(), saved.getId(), saved.getSystemId());
        return mapFunctionToResponse(saved);
    }

    @Transactional
    public SystemFunctionResponse updateFunction(String id, SystemFunctionRequest request) {
        SystemFunctionEntity entity = functionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("System function not found: " + id));
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSystemId(request.getSystemId());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        SystemFunctionEntity saved = functionRepo.save(entity);
        log.info("Updated system function: {} ({})", saved.getName(), saved.getId());
        return mapFunctionToResponse(saved);
    }

    @Transactional
    public void deactivateFunction(String id) {
        SystemFunctionEntity entity = functionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("System function not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        functionRepo.save(entity);
        log.info("Deactivated system function: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== Reporter Teams ====================

    @Transactional(readOnly = true)
    public List<ReporterTeamResponse> getAllReporterTeams() {
        return teamRepo.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .map(this::mapTeamToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporterTeamResponse> getReporterTeamsByProgram(String programId) {
        return teamRepo.findByProgramIdAndIsActiveTrueOrderByDisplayOrderAsc(programId).stream()
                .map(this::mapTeamToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReporterTeamResponse createReporterTeam(ReporterTeamRequest request) {
        ReporterTeamEntity entity = ReporterTeamEntity.builder()
                .id(UUID.randomUUID().toString())
                .code(request.getCode())
                .name(request.getName())
                .programId(request.getProgramId())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ReporterTeamEntity saved = teamRepo.save(entity);
        log.info("Created reporter team: {} ({})", saved.getName(), saved.getId());
        return mapTeamToResponse(saved);
    }

    @Transactional
    public ReporterTeamResponse updateReporterTeam(String id, ReporterTeamRequest request) {
        ReporterTeamEntity entity = teamRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporter team not found: " + id));
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setProgramId(request.getProgramId());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        ReporterTeamEntity saved = teamRepo.save(entity);
        log.info("Updated reporter team: {} ({})", saved.getName(), saved.getId());
        return mapTeamToResponse(saved);
    }

    @Transactional
    public void deactivateReporterTeam(String id) {
        ReporterTeamEntity entity = teamRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporter team not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        teamRepo.save(entity);
        log.info("Deactivated reporter team: {} ({})", entity.getName(), entity.getId());
    }

    // ==================== Test Mean Defect Origins ====================

    @Transactional(readOnly = true)
    public List<TestMeanDefectOriginResponse> getRootDefectOrigins() {
        return defectOriginRepo.findByParentIdIsNullAndIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapDefectOriginToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestMeanDefectOriginResponse> getDefectOriginSubItems(String parentId) {
        return defectOriginRepo.findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(parentId).stream()
                .map(this::mapDefectOriginToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TestMeanDefectOriginResponse createDefectOrigin(TestMeanDefectOriginRequest request) {
        TestMeanDefectOriginEntity entity = TestMeanDefectOriginEntity.builder()
                .id(UUID.randomUUID().toString())
                .category(request.getCategory())
                .subItem(request.getSubItem())
                .parentId(request.getParentId())
                .isActive(true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        TestMeanDefectOriginEntity saved = defectOriginRepo.save(entity);
        log.info("Created defect origin: {} ({}) parentId={}", saved.getCategory(), saved.getId(), saved.getParentId());
        return mapDefectOriginToResponse(saved);
    }

    @Transactional
    public TestMeanDefectOriginResponse updateDefectOrigin(String id, TestMeanDefectOriginRequest request) {
        TestMeanDefectOriginEntity entity = defectOriginRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Defect origin not found: " + id));
        entity.setCategory(request.getCategory());
        entity.setSubItem(request.getSubItem());
        entity.setParentId(request.getParentId());
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        TestMeanDefectOriginEntity saved = defectOriginRepo.save(entity);
        log.info("Updated defect origin: {} ({})", saved.getCategory(), saved.getId());
        return mapDefectOriginToResponse(saved);
    }

    @Transactional
    public void deactivateDefectOrigin(String id) {
        TestMeanDefectOriginEntity entity = defectOriginRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Defect origin not found: " + id));
        entity.setIsActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        defectOriginRepo.save(entity);
        log.info("Deactivated defect origin: {} ({})", entity.getCategory(), entity.getId());
    }

    // ==================== Private Mapping Methods ====================

    private AircraftProgramResponse mapProgramToResponse(AircraftProgramEntity entity) {
        return AircraftProgramResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .parentProgramId(entity.getParentProgramId())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TestMeanResponse mapTestMeanToResponse(TestMeanEntity entity) {
        return TestMeanResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .programId(entity.getProgramId())
                .category(entity.getCategory())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AircraftSystemResponse mapSystemToResponse(AircraftSystemEntity entity) {
        return AircraftSystemResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .programId(entity.getProgramId())
                .ataChapterCode(entity.getAtaChapterCode())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AtaChapterResponse mapAtaChapterToResponse(AtaChapterEntity entity) {
        return AtaChapterResponse.builder()
                .id(entity.getId())
                .chapterNumber(entity.getChapterNumber())
                .title(entity.getTitle())
                .programId(entity.getProgramId())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private SystemSupplierResponse mapSupplierToResponse(SystemSupplierEntity entity) {
        return SystemSupplierResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .programId(entity.getProgramId())
                .systemId(entity.getSystemId())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private SystemFunctionResponse mapFunctionToResponse(SystemFunctionEntity entity) {
        return SystemFunctionResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .systemId(entity.getSystemId())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ReporterTeamResponse mapTeamToResponse(ReporterTeamEntity entity) {
        return ReporterTeamResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .programId(entity.getProgramId())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TestMeanDefectOriginResponse mapDefectOriginToResponse(TestMeanDefectOriginEntity entity) {
        return TestMeanDefectOriginResponse.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .subItem(entity.getSubItem())
                .parentId(entity.getParentId())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
