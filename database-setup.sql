-- SignFlow Database Setup Script
-- Run this script in MariaDB to set up the database

-- Create database
CREATE DATABASE IF NOT EXISTS signflow_db;
USE signflow_db;

-- Users table will be auto-created by JPA, but here's the schema for reference:
-- The application uses JPA with ddl-auto=update, so tables will be created automatically
-- This script is for manual setup if needed

-- Note: The tables will be automatically created by Spring Boot JPA
-- when you run the application for the first time.
-- However, if you want to create them manually, use the following:

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    signature_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id),
    INDEX idx_status (status),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS signers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    signed_at TIMESTAMP NULL,
    INDEX idx_document (document_id),
    INDEX idx_email (email),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    performed_by BIGINT NOT NULL,
    ip_address VARCHAR(50),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document (document_id),
    INDEX idx_user (performed_by),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample data (optional - for testing)
-- Note: Password is 'password123' hashed with BCrypt
-- INSERT INTO users (name, email, password_hash, role, created_at, updated_at) 
-- VALUES ('Test User', 'test@signflow.com', '$2a$10$YourBCryptHashHere', 'USER', NOW(), NOW());

-- Show tables
SHOW TABLES;

-- Grant privileges (adjust as needed)
-- GRANT ALL PRIVILEGES ON signflow_db.* TO 'root'@'localhost';
-- FLUSH PRIVILEGES;

SELECT 'Database setup complete!' AS message;
