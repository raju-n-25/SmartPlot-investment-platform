package com.example.spip_draft.repository;

import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.BookingStatus;
import com.example.spip_draft.enums.PlotApprovalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PlotRepository extends JpaRepository<Plot, Long>, JpaSpecificationExecutor<Plot> {

    List<Plot> findByProjectId(Long projectId);

    List<Plot> findByTenantId(Long tenantId);

    List<Plot> findByStatusAndAvailableAndBookingStatus(
            PlotApprovalStatus status,
            Boolean available,
            BookingStatus bookingStatus
    );

    @Query("""	
        SELECT DISTINCT p FROM Plot p
        JOIN p.project pr
        LEFT JOIN FETCH p.images imgs
        WHERE pr.status = com.example.spip_draft.enums.ProjectStatus.APPROVED
        AND p.status = com.example.spip_draft.enums.PlotApprovalStatus.APPROVED
        AND p.available = true
        AND p.bookingStatus IS NULL
        ORDER BY p.id DESC, imgs.displayOrder ASC
    """)
    List<Plot> findVisiblePlots();

    List<Plot> findByPriceLessThanEqualAndStatus(BigDecimal price, PlotApprovalStatus status);

    List<Plot> findByAreaGreaterThanEqualAndStatus(Double area, PlotApprovalStatus status);

    List<Plot> findByFacingAndStatus(String facing, PlotApprovalStatus status);

    List<Plot> findByCornerPlotAndStatus(Boolean cornerPlot, PlotApprovalStatus status);

    boolean existsByIdAndStatus(Long id, PlotApprovalStatus status);

    @Query("""
    	    SELECT DISTINCT p FROM Plot p
    	    JOIN p.project pr
    	    LEFT JOIN FETCH p.images imgs
    	    WHERE p.id = :id
    	    AND pr.status = com.example.spip_draft.enums.ProjectStatus.APPROVED
    	    AND p.status = com.example.spip_draft.enums.PlotApprovalStatus.APPROVED
    	    AND p.available = true
    	    AND p.bookingStatus IS NULL
    	    ORDER BY imgs.displayOrder ASC
    	""")
    	Optional<Plot> findBookablePlot(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Plot p WHERE p.id = :id")
    Optional<Plot> findPlotForUpdate(@Param("id") Long id);

    @Query("""
        SELECT p FROM Plot p
        JOIN FETCH p.project pr
        WHERE pr.builder.id = :builderId
    """)
    List<Plot> findBuilderPlots(@Param("builderId") Long builderId);

    long countByProject_Builder_Id(Long builderId);

    long countByProject_Builder_IdAndAvailableTrue(Long builderId);

    long countByProject_Builder_IdAndBookingStatus(Long builderId, BookingStatus bookingStatus);

    boolean existsByProjectIdAndPlotNumber(Long projectId, String plotNumber);

    boolean existsByProjectId(Long projectId);

    @Query("""
    	    SELECT DISTINCT p FROM Plot p
    	    JOIN p.project pr
    	    LEFT JOIN FETCH p.images imgs
    	    WHERE p.id = :id
    	    AND pr.status = com.example.spip_draft.enums.ProjectStatus.APPROVED
    	    AND p.status = com.example.spip_draft.enums.PlotApprovalStatus.APPROVED
    	    ORDER BY imgs.displayOrder ASC
    	""")
    	Optional<Plot> findVisiblePlotByIdWithImages(@Param("id") Long id);

    boolean existsByProject_IdAndPlotNumberIgnoreCase(Long projectId, String plotNumber);

    long countByProjectId(Long projectId);

    long countByProjectIdAndAvailableTrue(Long projectId);

    long countByProjectIdAndBookingStatus(Long projectId, BookingStatus bookingStatus);

    List<Plot> findByProject_Builder_Id(Long builderId);
    boolean existsByIdAndAvailableTrue(Long plotId);
}
