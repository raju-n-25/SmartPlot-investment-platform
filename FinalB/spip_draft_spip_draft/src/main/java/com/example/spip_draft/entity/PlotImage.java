package com.example.spip_draft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "plot_images")
@Getter
@Setter
public class PlotImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    // Optional: mark main image
    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    // Optional: for ordering images later
    @Column(name = "display_order")
    private Integer displayOrder;
}