package com.example.spip_draft.repository;

import com.example.spip_draft.entity.ProjectAmenity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectAmenityRepository extends JpaRepository<ProjectAmenity, Long> {

    List<ProjectAmenity> findByProject_Id(Long projectId);
    Optional<ProjectAmenity> findByIdAndProject_Id(Long amenityId, Long projectId);
}