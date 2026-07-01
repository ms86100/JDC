package com.jira.sprint;

import com.jira.sprint.repository.AgileBoardRepository;
import com.jira.sprint.repository.BoardColumnRepository;
import com.jira.sprint.repository.BoardConfigRepository;
import com.jira.sprint.repository.BoardSprintRepository;
import com.jira.sprint.service.AgileBoardService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(
        basePackages = {"com.jira.sprint", "com.jira.board"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = AgileBoardService.class))
@EntityScan(basePackages = {"com.jira.board.entity", "com.jira.sprint.entity"})
@EnableJpaRepositories(
        basePackages = {"com.jira.board.repository", "com.jira.sprint.repository"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AgileBoardRepository.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BoardColumnRepository.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BoardSprintRepository.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BoardConfigRepository.class),
        })
public class JiraSprintServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JiraSprintServiceApplication.class, args);
    }
}
