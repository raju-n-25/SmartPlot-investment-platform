CREATE TABLE IF NOT EXISTS layout_files_v2 (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL UNIQUE,
    file_path VARCHAR(1000) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    converted_image_path VARCHAR(1000),
    processing_status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    processing_error VARCHAR(2000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_layout_files_v2_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_layout_v2_project_id ON layout_files_v2(project_id);
CREATE INDEX idx_layout_v2_status ON layout_files_v2(processing_status);

CREATE TABLE IF NOT EXISTS layout_processing_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    layout_file_id BIGINT NOT NULL UNIQUE,
    total_plots_detected INT,
    scale_factor_px_per_sqft DOUBLE,
    image_width INT,
    image_height INT,
    debug_grayscale_path VARCHAR(1000),
    debug_edge_path VARCHAR(1000),
    debug_contour_path VARCHAR(1000),
    debug_filtered_plots_path VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lpr_layout_file
        FOREIGN KEY (layout_file_id) REFERENCES layout_files_v2(id) ON DELETE CASCADE
);

CREATE INDEX idx_lpr_layout_file_id ON layout_processing_results(layout_file_id);

CREATE TABLE IF NOT EXISTS detected_plots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    processing_result_id BIGINT NOT NULL,
    plot_index INT NOT NULL,
    bbox_x INT,
    bbox_y INT,
    bbox_width INT,
    bbox_height INT,
    pixel_area DOUBLE,
    area_sqft DOUBLE,
    area_sqm DOUBLE,
    is_rectangle BOOLEAN DEFAULT FALSE,
    corner_points_json VARCHAR(2000),
    linked_plot_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dp_result
        FOREIGN KEY (processing_result_id) REFERENCES layout_processing_results(id) ON DELETE CASCADE
);

CREATE INDEX idx_dp_result_id ON detected_plots(processing_result_id);
CREATE INDEX idx_dp_plot_index ON detected_plots(plot_index);