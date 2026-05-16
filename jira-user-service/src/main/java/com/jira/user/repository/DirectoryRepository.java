package com.jira.user.repository;

import com.jira.user.entity.Directory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DirectoryRepository extends JpaRepository<Directory, UUID> {

    Optional<Directory> findByDirectoryTypeAndIsActiveTrue(String directoryType);

    List<Directory> findByIsActiveTrueOrderByOrderIndexAsc();

    boolean existsByDirectoryName(String directoryName);
}