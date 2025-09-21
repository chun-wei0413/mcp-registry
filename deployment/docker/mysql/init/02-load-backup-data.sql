-- Load sample database backup files
-- This script loads the actual backup data after database initialization

USE sample_database;

-- Disable foreign key checks temporarily to allow loading in any order
SET FOREIGN_KEY_CHECKS = 0;

-- Clear existing sample data first
DELETE FROM comments;
DELETE FROM attachments;
DELETE FROM cards;
DELETE FROM lists;
DELETE FROM boards;
DELETE FROM projects;
DELETE FROM users;

-- Note: The actual backup data will be loaded via MySQL command line
-- after container startup using the backup files in /backup_data

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'Ready to load backup data from /backup_data' as status;