-- ==============================================================================
-- DATABASE 2: NEOPAT SMS ISOLATED DATABASE INITIALIZATION
-- Database Name: neopa_sms
-- Dedicated to Neopat Assessment -> Parent SMS Integration
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS neopa_sms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE neopa_sms;

-- 1. current_assessment: Temporary storage for newly received Neopat assessments
CREATE TABLE IF NOT EXISTS current_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    test_id VARCHAR(100) NOT NULL,
    marks DECIMAL(7,2) NOT NULL,
    total_marks DECIMAL(7,2) NOT NULL,
    attempts INT DEFAULT 1,
    result_analysis_url VARCHAR(1024),
    start_time DATETIME NULL,
    submit_time DATETIME NULL,
    section_wise_marks JSON NULL,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_curr_email (email),
    INDEX idx_curr_test_id (test_id),
    INDEX idx_curr_status (processing_status)
) ENGINE=InnoDB;

-- 2. assessment_history: Permanent archive of successfully processed assessments
CREATE TABLE IF NOT EXISTS assessment_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_assessment_id BIGINT NULL,
    email VARCHAR(255) NOT NULL,
    test_id VARCHAR(100) NOT NULL,
    marks DECIMAL(7,2) NOT NULL,
    total_marks DECIMAL(7,2) NOT NULL,
    attempts INT DEFAULT 1,
    result_analysis_url VARCHAR(1024),
    start_time DATETIME NULL,
    submit_time DATETIME NULL,
    section_wise_marks JSON NULL,
    sms_status VARCHAR(50) NOT NULL, -- 'SENT', 'FAILED'
    sms_failure_reason VARCHAR(500) NULL,
    sms_sent_at DATETIME NULL,
    processed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hist_email (email),
    INDEX idx_hist_test_id (test_id),
    INDEX idx_hist_sms_status (sms_status)
) ENGINE=InnoDB;

-- 3. failed_assessment: Store assessment processing failures
CREATE TABLE IF NOT EXISTS failed_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_assessment_id BIGINT NULL,
    email VARCHAR(255) NOT NULL,
    test_id VARCHAR(100) NULL,
    marks DECIMAL(7,2) NULL,
    total_marks DECIMAL(7,2) NULL,
    attempts INT DEFAULT 1,
    result_analysis_url VARCHAR(1024),
    start_time DATETIME NULL,
    submit_time DATETIME NULL,
    section_wise_marks JSON NULL,
    failure_type VARCHAR(100) NOT NULL, -- INVALID_PAYLOAD, STUDENT_NOT_FOUND, PARENT_CONTACT_NOT_FOUND, DUPLICATE_ASSESSMENT, INVALID_MARKS, DATABASE_ERROR, UNKNOWN_ERROR
    failure_reason VARCHAR(1000) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_retry_at DATETIME NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    INDEX idx_fail_email (email),
    INDEX idx_fail_type (failure_type),
    INDEX idx_fail_status (status)
) ENGINE=InnoDB;

-- 4. parent_contact: Store parent contact info required for SMS
CREATE TABLE IF NOT EXISTS parent_contact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_email VARCHAR(255) NOT NULL,
    parent_mobile VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_parent_student_email (student_email),
    INDEX idx_parent_active (is_active)
) ENGINE=InnoDB;

-- 5. audit_log: Track integration events
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NULL,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    message VARCHAR(1000) NULL,
    metadata JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_event (event_type),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB;

-- 6. neopat_sms_schedule: Store Super Admin's weekly SMS schedule
CREATE TABLE IF NOT EXISTS neopat_sms_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_of_week VARCHAR(20) NOT NULL DEFAULT 'SATURDAY',
    send_time TIME NOT NULL DEFAULT '10:00:00',
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    timezone VARCHAR(100) NOT NULL DEFAULT 'Asia/Kolkata',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NULL
) ENGINE=InnoDB;

-- Ensure default single schedule record exists (id=1, disabled by default)
INSERT INTO neopat_sms_schedule (id, day_of_week, send_time, enabled, timezone)
VALUES (1, 'SATURDAY', '10:00:00', FALSE, 'Asia/Kolkata')
ON DUPLICATE KEY UPDATE id=id;

-- 7. neopat_sms_execution: Prevent duplicate weekly SMS execution
CREATE TABLE IF NOT EXISTS neopat_sms_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    status VARCHAR(50) NOT NULL, -- IN_PROGRESS, COMPLETED, FAILED
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exec_date_time (scheduled_date, scheduled_time)
) ENGINE=InnoDB;
