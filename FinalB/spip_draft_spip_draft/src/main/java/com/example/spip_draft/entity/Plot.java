package com.example.spip_draft.entity;

import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.KathaType;
import com.example.spip_draft.enums.RiskCategory;
import com.example.spip_draft.enums.PlotApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "plot_number"}))
@Getter
@Setter
public class Plot extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "plot_number", nullable = false)
    private String plotNumber;

    @Column(nullable = false)
    private Double area;

    @Column(nullable = false)
    private BigDecimal price;

    private String facing;

    @Column(name = "corner_plot")
    private Boolean cornerPlot = false;

    @Column(name = "legal_info")
    private String legalInfo;

    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    @Column(name = "plot_description", length = 2000)
    private String plotDescription;

    @Column(name = "plot_dimensions", length = 100)
    private String plotDimensions;

    @Column(name = "road_width")
    private Double roadWidth;

    @Column(name = "map_link", length = 1000)
    private String mapLink;

    @Column(name = "primary_image_url", length = 1000)
    private String primaryImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlotApprovalStatus status = PlotApprovalStatus.PENDING;

    @Column(nullable = false)
    private Boolean available = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    private BookingStatus bookingStatus;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToMany(mappedBy = "plot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlotImage> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "khata_type")
    private KathaType khataType;

    @Column(name = "khata_remarks", length = 1000)
    private String khataRemarks;

    @Column(name = "plot_risk_score")
    private Integer plotRiskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "plot_risk_category")
    private RiskCategory plotRiskCategory = RiskCategory.LOW;

    public boolean isVisible() {
        return project != null
                && project.getStatus() != null
                && project.getStatus().isApproved()
                && status == PlotApprovalStatus.APPROVED
                && Boolean.TRUE.equals(available)
                && bookingStatus == null;
    }

    public boolean isBookable() {
        return isVisible();
    }

    public boolean isBlocked() {
        return bookingStatus == BookingStatus.TOKEN_BLOCKED
                && blockedAt != null;
    }

    public boolean isSold() {
        return bookingStatus == BookingStatus.CONFIRMED;
    }
    
    @Column(name = "coordinates", columnDefinition = "LONGTEXT")
    private String coordinates;

    @Column(name = "shape_type", length = 20)
    private String shapeType;

    @Column(name = "road_facing", nullable = false)
    private Boolean roadFacing = false;
}