-- Partition audit_logs by created_at for time-based archival
-- Range partitioning by month enables efficient archival of old data

-- Create partitioned audit table
CREATE TABLE IF NOT EXISTS audit_logs_partitioned (
    LIKE audit_logs INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- Create monthly partitions for current year
CREATE TABLE IF NOT EXISTS audit_logs_2026_q1 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
CREATE TABLE IF NOT EXISTS audit_logs_2026_q2 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE IF NOT EXISTS audit_logs_2026_q3 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE IF NOT EXISTS audit_logs_2026_q4 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS audit_logs_2027_q1 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2027-01-01') TO ('2027-04-01');

-- Default partition for future dates
CREATE TABLE IF NOT EXISTS audit_logs_future PARTITION OF audit_logs_partitioned DEFAULT;

COMMENT ON TABLE audit_logs IS 'Partition-ready: use audit_logs_partitioned for RANGE partitioning by created_at when data exceeds 1M rows';
