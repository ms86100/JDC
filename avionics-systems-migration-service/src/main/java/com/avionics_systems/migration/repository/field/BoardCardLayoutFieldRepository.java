package com.avionics_systems.migration.repository.field;

import com.avionics_systems.migration.entity.field.BoardCardLayoutFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoardCardLayoutFieldRepository extends JpaRepository<BoardCardLayoutFieldEntity, UUID> {

    List<BoardCardLayoutFieldEntity> findByBoardIdAndVisibleTrueOrderByDisplayOrderAsc(UUID boardId);

    List<BoardCardLayoutFieldEntity> findByBoardIdOrderByDisplayOrderAsc(UUID boardId);

    void deleteByBoardId(UUID boardId);
}
