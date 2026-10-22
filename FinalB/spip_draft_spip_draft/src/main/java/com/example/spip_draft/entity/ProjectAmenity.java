package com.example.spip_draft.entity;

import com.example.spip_draft.enums.AmenityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "project_amenities")
@Getter
@Setter
public class ProjectAmenity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "amenity_type", nullable = false)
    private AmenityType amenityType;

    @Column(name = "description", length = 1000)
    private String description;
}