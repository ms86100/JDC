package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.I18nMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface I18nMessageRepository extends JpaRepository<I18nMessageEntity, UUID> {

    Optional<I18nMessageEntity> findByMessageKeyAndLocale(String messageKey, String locale);

    List<I18nMessageEntity> findByLocaleOrderByMessageKeyAsc(String locale);

    List<I18nMessageEntity> findByCategoryAndLocaleOrderByMessageKeyAsc(String category, String locale);
}
