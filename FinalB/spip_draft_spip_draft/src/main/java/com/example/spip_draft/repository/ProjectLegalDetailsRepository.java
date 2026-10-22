package com.example.spip_draft.repository;

import com.example.spip_draft.entity.ProjectLegalDetails;
import com.example.spip_draft.enums.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectLegalDetailsRepository extends JpaRepository<ProjectLegalDetails, Long> {

    Optional<ProjectLegalDetails> findByProject_Id(Long projectId);

    boolean existsByProject_Id(Long projectId);

    List<ProjectLegalDetails> findByProject_Builder_Id(Long builderId);

    long countByProject_Builder_IdAndProjectRiskCategory(Long builderId, RiskCategory riskCategory);

    long countByProjectRiskCategory(RiskCategory riskCategory);
}