package com.example.spip_draft.dto.complaint;

import com.example.spip_draft.enums.ComplaintType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplaintRequest {

    private ComplaintType complaintType = ComplaintType.GENERAL;

    private String reason;

    private String description;

    private Boolean escrowRelated = false;

    private Boolean paymentRelated = false;

    private Boolean agreementRelated = false;
}