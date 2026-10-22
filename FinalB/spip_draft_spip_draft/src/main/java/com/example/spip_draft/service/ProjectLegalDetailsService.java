package com.example.spip_draft.service;

import com.example.spip_draft.dto.legal.ProjectLegalDetailsRequest;
import com.example.spip_draft.dto.legal.ProjectLegalDetailsResponse;
import com.example.spip_draft.dto.legal.ProjectLegalReviewResponse;

public interface ProjectLegalDetailsService {

    ProjectLegalDetailsResponse saveOrUpdate(Long projectId, ProjectLegalDetailsRequest request);

    ProjectLegalDetailsResponse getByProjectId(Long projectId);

    ProjectLegalReviewResponse reviewProject(Long projectId);
}