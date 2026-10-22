package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Project;
import com.example.spip_draft.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByBuilderId(Long builderId);

    List<Project> findByStatus(ProjectStatus status);

    List<Project> findByBuilderIdAndStatus(Long builderId, ProjectStatus status);

    // ✅ ADD THIS (VERY IMPORTANT)
    Optional<Project> findByIdAndBuilderId(Long projectId, Long builderId);

    // ✅ ADD THIS (used sometimes for validation checks)
    boolean existsByIdAndBuilderId(Long projectId, Long builderId);
}