package com.example.spip_draft.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
public class PaginationRequest {

    @Min(value = 0, message = "Page index cannot be negative")
    private int page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size cannot exceed 100")
    private int size = 10;

    private String sortBy = "id";

    @Pattern(regexp = "ASC|DESC", message = "Direction must be ASC or DESC")
    private String direction = "ASC";

    // =========================
    // 🔥 HELPER METHOD
    // =========================

    public Pageable toPageable() {

        Sort sort = Sort.by(
                Sort.Direction.fromString(direction),
                sortBy
        );

        return PageRequest.of(page, size, sort);
    }
}