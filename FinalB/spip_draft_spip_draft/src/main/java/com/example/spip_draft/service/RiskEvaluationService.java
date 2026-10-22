package com.example.spip_draft.service;

import com.example.spip_draft.dto.legal.PlotRiskEvaluationResult;
import com.example.spip_draft.dto.legal.ProjectRiskEvaluationResult;
import com.example.spip_draft.entity.ProjectLegalDetails;
import com.example.spip_draft.enums.KathaType;

public interface RiskEvaluationService {

    ProjectRiskEvaluationResult evaluateProjectRisk(ProjectLegalDetails legalDetails);

    PlotRiskEvaluationResult evaluatePlotRisk(Integer projectRiskScore, KathaType khataType);
}