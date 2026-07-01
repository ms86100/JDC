package com.jira.workflow.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SPI for navigator extensions (activity tabs, detail panels, project nav items).
 */
@Component
@Slf4j
public class NavigatorPluginRegistry {

    public record NavigatorPluginDescriptor(
            String id,
            String name,
            String slot,
            int order) {}

    private final List<NavigatorPluginDescriptor> plugins = new CopyOnWriteArrayList<>();

    public void register(NavigatorPluginDescriptor descriptor) {
        plugins.add(descriptor);
        log.info("Registered navigator plugin {} in slot {}", descriptor.id(), descriptor.slot());
    }

    public List<NavigatorPluginDescriptor> listBySlot(String slot) {
        return plugins.stream()
                .filter(p -> slot.equalsIgnoreCase(p.slot()))
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .toList();
    }

    public List<NavigatorPluginDescriptor> listAll() {
        return Collections.unmodifiableList(new ArrayList<>(plugins));
    }
}
