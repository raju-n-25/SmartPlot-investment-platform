package com.example.spip_draft.dto.complaint;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintActionRequest {

    private String note;

    private String resolutionNote;

    private String rejectionReason;
}