package com.example.spip_draft.dto.payment;

import com.example.spip_draft.enums.RefundMode;
import lombok.Data;

@Data
public class RefundRequest {
    private RefundMode mode;
}