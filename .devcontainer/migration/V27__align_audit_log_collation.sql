-- V20 inherited the database collation, while V25 explicitly used unicode_ci.
-- Align both join keys and future audit columns without deleting existing logs.
ALTER TABLE audit_log
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
