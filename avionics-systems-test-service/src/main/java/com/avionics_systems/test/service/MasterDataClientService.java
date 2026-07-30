package com.avionics_systems.test.service;

import com.avionics_systems.test.config.AdminServiceClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDataClientService {

    private final RestTemplate adminRestTemplate;
    private final AdminServiceClientConfig config;

    @Cacheable(value = "masterdata-programs", key = "'all'")
    public List<Map<String, Object>> getAllPrograms() {
        String url = config.getAdminServiceUrl() + "/api/admin/master-data/programs";
        log.debug("Fetching all programs from admin service: {}", url);
        try {
            var response = adminRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch programs from admin service: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "masterdata-test-means", key = "#programId")
    public List<Map<String, Object>> getTestMeansByProgram(String programId) {
        String url = config.getAdminServiceUrl() + "/api/admin/master-data/programs/" + programId + "/test-means";
        log.debug("Fetching test means for program {} from admin service: {}", programId, url);
        try {
            var response = adminRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch test means for program {} from admin service: {}", programId, e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "masterdata-systems", key = "#programId")
    public List<Map<String, Object>> getSystemsByProgram(String programId) {
        String url = config.getAdminServiceUrl() + "/api/admin/master-data/programs/" + programId + "/systems";
        log.debug("Fetching systems for program {} from admin service: {}", programId, url);
        try {
            var response = adminRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch systems for program {} from admin service: {}", programId, e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "masterdata-suppliers", key = "#programId + ':' + #systemId")
    public List<Map<String, Object>> getSuppliersByProgramAndSystem(String programId, String systemId) {
        String url = config.getAdminServiceUrl() + "/api/admin/master-data/programs/" + programId
                + "/systems/" + systemId + "/suppliers";
        log.debug("Fetching suppliers for program {} system {} from admin service: {}", programId, systemId, url);
        try {
            var response = adminRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch suppliers for program {} system {} from admin service: {}",
                    programId, systemId, e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "masterdata-functions", key = "#systemId")
    public List<Map<String, Object>> getFunctionsBySystem(String systemId) {
        String url = config.getAdminServiceUrl() + "/api/admin/master-data/systems/" + systemId + "/functions";
        log.debug("Fetching functions for system {} from admin service: {}", systemId, url);
        try {
            var response = adminRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch functions for system {} from admin service: {}", systemId, e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "masterdata-reporter-teams", key = "'all'")
    public List<Map<String, Object>> getReporterTeams() {
        String url = config.getAdminServiceUrl() + "/api/admin/master-data/reporter-teams";
        log.debug("Fetching reporter teams from admin service: {}", url);
        try {
            var response = adminRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch reporter teams from admin service: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "masterdata-defect-origins", key = "'roots'")
    public List<Map<String, Object>> getRootDefectOrigins() {
        String url = config.getAdminServiceUrl() + "/api/admin/master-data/defect-origins/roots";
        log.debug("Fetching root defect origins from admin service: {}", url);
        try {
            var response = adminRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch root defect origins from admin service: {}", e.getMessage());
            return List.of();
        }
    }
}
