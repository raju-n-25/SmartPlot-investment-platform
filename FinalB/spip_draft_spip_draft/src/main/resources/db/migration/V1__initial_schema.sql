-- USERS TABLE (Authentication Table)
CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       full_name VARCHAR(150) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role ENUM('ROLE_ADMIN','ROLE_BUILDER','ROLE_INVESTOR') NOT NULL,
                       active BOOLEAN DEFAULT TRUE,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- BUILDERS TABLE
CREATE TABLE builders (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          user_id BIGINT NOT NULL UNIQUE,
                          company_name VARCHAR(150) NOT NULL,
                          kyc_document_url VARCHAR(255),
                          kyc_status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
                          rating DOUBLE DEFAULT 0,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- INVESTORS TABLE
CREATE TABLE investors (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           user_id BIGINT NOT NULL UNIQUE,
                           pan_card VARCHAR(20) NOT NULL UNIQUE,
                           wallet_balance DECIMAL(12,2) DEFAULT 0 CHECK (wallet_balance >= 0),
                           risk_score INT DEFAULT 0,
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- PROJECTS TABLE
CREATE TABLE projects (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          builder_id BIGINT NOT NULL,
                          tenant_id BIGINT NOT NULL,
                          project_name VARCHAR(150) NOT NULL,
                          description TEXT,
                          location VARCHAR(150),
                          city VARCHAR(100),
                          state VARCHAR(100),
                          total_plots INT NOT NULL,
                          available_plots INT NOT NULL,
                          status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          FOREIGN KEY (builder_id) REFERENCES builders(id) ON DELETE CASCADE,
                          INDEX idx_projects_tenant (tenant_id)
);

-- PLOTS TABLE
CREATE TABLE plots (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       project_id BIGINT NOT NULL,
                       tenant_id BIGINT NOT NULL,
                       plot_number VARCHAR(50) NOT NULL,
                       area DOUBLE NOT NULL,
                       price DECIMAL(12,2) NOT NULL,
                       available BOOLEAN DEFAULT TRUE,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,

                       UNIQUE (project_id, plot_number),
                       INDEX idx_plots_tenant (tenant_id)
);

-- BOOKINGS TABLE
CREATE TABLE bookings (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          plot_id BIGINT NOT NULL UNIQUE,
                          investor_id BIGINT NOT NULL,
                          tenant_id BIGINT NOT NULL,
                          booking_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                          booking_status ENUM('PENDING','CONFIRMED','CANCELLED') DEFAULT 'PENDING',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE CASCADE,
                          FOREIGN KEY (investor_id) REFERENCES investors(id) ON DELETE CASCADE,

                          INDEX idx_bookings_tenant (tenant_id)
);

-- PAYMENTS TABLE
CREATE TABLE payments (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          booking_id BIGINT NOT NULL UNIQUE,
                          tenant_id BIGINT NOT NULL,
                          amount DECIMAL(12,2) NOT NULL,
                          payment_type ENUM('UPI','CARD','NETBANKING','WALLET'),
                          payment_status ENUM('PENDING','SUCCESS','FAILED') DEFAULT 'PENDING',
                          transaction_id VARCHAR(150) UNIQUE,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                          FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,

                          INDEX idx_payments_tenant (tenant_id)
);

-- LEGAL DETAILS TABLE
CREATE TABLE legal_details (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               plot_id BIGINT NOT NULL UNIQUE,
                               katha_type ENUM('A','B','E-KATHA'),
                               rera_approved BOOLEAN DEFAULT FALSE,
                               dc_converted BOOLEAN DEFAULT FALSE,
                               risk_score INT DEFAULT 0,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                               FOREIGN KEY (plot_id) REFERENCES plots(id) ON DELETE CASCADE
);

-- NOTIFICATIONS TABLE
CREATE TABLE notifications (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               title VARCHAR(150) NOT NULL,
                               message TEXT NOT NULL,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                               FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- AUDIT LOG TABLE
CREATE TABLE audit_logs (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            action VARCHAR(150) NOT NULL,
                            entity_type VARCHAR(100),
                            entity_id BIGINT,
                            performed_by BIGINT,
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE booking_conflicts (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   plot_id BIGINT,
                                   investor_id BIGINT,
                                   reason VARCHAR(255),
                                   attempt_time DATETIME,
                                   created_at DATETIME,
                                   updated_at DATETIME   -- ✅ ADD THIS
);

INSERT INTO users (full_name, email, password, role)
VALUES (
           'Admin User',
           'admin@spip.com',
           '$2a$12$xFtodIdpfyua6g9Y9dARtOkUZshAdSfYsuhODiHD4sxaetPvp7shS',
           'ROLE_ADMIN'
       );