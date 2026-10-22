CREATE TABLE plot_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plot_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    display_order INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_plot_images_plot
        FOREIGN KEY (plot_id)
        REFERENCES plots(id)
        ON DELETE CASCADE
);