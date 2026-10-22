package com.example.spip_draft.entity;

import com.example.spip_draft.enums.ProjectStatus;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class Project extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "builder_id", nullable = false)
    private Builder builder;
    
    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ProjectLegalDetails legalDetails;
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProjectDocument> documents = new ArrayList<>();
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProjectAmenity> amenities = new ArrayList<>();

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(length = 1000)
    private String description;

    private String location;
    private String city;
    private String state;

    @Column(name = "total_plots")
    private Integer totalPlots = 0;

    @Column(name = "available_plots")
    private Integer availablePlots = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.PENDING;
}