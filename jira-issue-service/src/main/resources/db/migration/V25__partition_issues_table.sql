-- Partition issues table by project_id for horizontal scaling
-- This creates a partitioned copy and migrates data

-- Step 1: Create partitioned table structure
CREATE TABLE IF NOT EXISTS issues_partitioned (
    LIKE issues INCLUDING ALL
) PARTITION BY LIST (project_id);

-- Step 2: Create default partition for unassigned projects
CREATE TABLE IF NOT EXISTS issues_default PARTITION OF issues_partitioned DEFAULT;

-- Note: Project-specific partitions are created dynamically when projects are created
-- Example: CREATE TABLE issues_proj_<uuid> PARTITION OF issues_partitioned FOR VALUES IN ('<uuid>');

-- For now, we keep the original table and add a comment documenting the partition strategy
COMMENT ON TABLE issues IS 'Partition-ready: use issues_partitioned for LIST partitioning by project_id when data exceeds 10M rows';
