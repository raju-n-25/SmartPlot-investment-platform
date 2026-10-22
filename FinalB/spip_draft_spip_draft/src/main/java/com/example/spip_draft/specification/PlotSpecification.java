package com.example.spip_draft.specification;

import com.example.spip_draft.dto.marketplace.PlotFilterRequest;
import com.example.spip_draft.entity.Plot;
import com.example.spip_draft.enums.ProjectStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PlotSpecification {

    public static Specification<Plot> filterPlots(PlotFilterRequest req) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("project").get("status"), ProjectStatus.APPROVED));
            predicates.add(cb.isTrue(root.get("available")));
            predicates.add(cb.isNull(root.get("bookingStatus")));

            if (req.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), req.getMinPrice()));
            }

            if (req.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), req.getMaxPrice()));
            }

            if (req.getMinArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), req.getMinArea()));
            }

            if (req.getMaxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), req.getMaxArea()));
            }

            if (req.getFacing() != null && !req.getFacing().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("facing")),
                        req.getFacing().toLowerCase()
                ));
            }

            if (req.getCornerPlot() != null) {
                predicates.add(cb.equal(root.get("cornerPlot"), req.getCornerPlot()));
            }

            if (req.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), req.getProjectId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}