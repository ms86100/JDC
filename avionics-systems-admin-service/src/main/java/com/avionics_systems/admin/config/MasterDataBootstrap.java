package com.avionics_systems.admin.config;

import com.avionics_systems.admin.repository.AircraftProgramRepository;
import com.avionics_systems.admin.repository.AircraftSystemRepository;
import com.avionics_systems.admin.repository.ReporterTeamRepository;
import com.avionics_systems.admin.repository.TestMeanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MasterDataBootstrap implements ApplicationRunner {

    private final AircraftProgramRepository programRepo;
    private final TestMeanRepository testMeanRepo;
    private final AircraftSystemRepository systemRepo;
    private final ReporterTeamRepository teamRepo;

    @Override
    public void run(ApplicationArguments args) {
        long programs = programRepo.count();
        long testMeans = testMeanRepo.count();
        long systems = systemRepo.count();
        long teams = teamRepo.count();
        log.info("Master data loaded: {} programs, {} test means, {} systems, {} reporter teams",
                programs, testMeans, systems, teams);
    }
}
