package com.example.spip_draft.plotintelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContourPoint {
    private Double x;
    private Double y;

    public double[] toArray() {
        return new double[]{
                x != null ? x : 0.0,
                y != null ? y : 0.0
        };
    }
}