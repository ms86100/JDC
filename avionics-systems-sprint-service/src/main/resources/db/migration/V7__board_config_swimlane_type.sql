-- Task 3.2.1: Add swimlane_type column to board_configs
ALTER TABLE board_configs ADD COLUMN IF NOT EXISTS swimlane_type VARCHAR(30) DEFAULT 'NONE';
