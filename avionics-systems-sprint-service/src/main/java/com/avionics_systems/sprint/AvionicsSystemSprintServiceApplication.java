package com.avionics_systems.sprint;

import com.avionics_systems.sprint.repository.AgileBoardRepository;
import com.avionics_systems.sprint.repository.BoardColumnRepository;
import com.avionics_systems.sprint.repository.BoardConfigRepository;
import com.avionics_systems.sprint.repository.BoardSprintRepository;
import com.avionics_systems.sprint.service.AgileBoardService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(
        basePackages = {"com.avionics_systems.sprint", "com.avionics_systems.board"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AgileBoardService.class))
@EntityScan(basePackages = {"com.avionics_systems.board.entity", "com.avionics_systems.sprint.entity"})
@EnableJpaRepositories(
        basePackages = {"com.avionics_systems.board.repository", "com.avionics_systems.sprint.repository"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AgileBoardRepository.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BoardColumnRepository.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BoardSprintRepository.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BoardConfigRepository.class),
        })
public class AvionicsSystemSprintServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemSprintServiceApplication.class, args);
    }
}
