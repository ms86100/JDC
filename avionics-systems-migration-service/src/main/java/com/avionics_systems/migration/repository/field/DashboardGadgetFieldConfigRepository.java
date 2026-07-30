package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.DashboardGadgetFieldConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DashboardGadgetFieldConfigRepository extends JpaRepository<DashboardGadgetFieldConfigEntity, UUID> {

    List<DashboardGadgetFieldConfigEntity> findByDashboardKeyAndGadgetKeyAndEnabledTrueOrderByDisplayOrderAsc(
            String dashboardKey, String gadgetKey);

    List<DashboardGadgetFieldConfigEntity> findByDashboardKeyAndGadgetKeyOrderByDisplayOrderAsc(
            String dashboardKey, String gadgetKey);

    void deleteByDashboardKeyAndGadgetKey(String dashboardKey, String gadgetKey);
}
