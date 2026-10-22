package com.example.spip_draft.service;

import com.example.spip_draft.dto.legal.LegalDetailsRequest;
import com.example.spip_draft.dto.legal.LegalDetailsResponse;

public interface LegalDetailsService {

    LegalDetailsResponse create(LegalDetailsRequest request);

    LegalDetailsResponse getByPlotId(Long plotId);

    LegalDetailsResponse update(Long id, LegalDetailsRequest request);

    void delete(Long id);
}