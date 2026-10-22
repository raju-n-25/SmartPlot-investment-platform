package com.example.spip_draft.dto.legal;

import com.example.spip_draft.enums.RiskCategory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlotRiskEvaluationResult {

    private Integer plotRiskScore;
    private RiskCategory plotRiskCategory;
}