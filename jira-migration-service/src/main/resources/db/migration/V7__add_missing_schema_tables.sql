-- V7__add_missing_schema_tables.sql
-- Cross-service tables/indexes are owned by each domain service's Flyway migrations.
-- Only create indexes when the target table AND columns already exist.

CREATE OR REPLACE FUNCTION jira_migration._create_index_if_columns_exist(
    p_schema text,
    p_table text,
    p_index text,
    p_columns text
) RETURNS void AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = p_schema AND table_name = p_table
    ) THEN
        RETURN;
    END IF;
    IF EXISTS (
        SELECT 1
        FROM unnest(string_to_array(replace(p_columns, ' ', ''), ',')) AS col(name)
        WHERE NOT EXISTS (
            SELECT 1 FROM information_schema.columns c
            WHERE c.table_schema = p_schema AND c.table_name = p_table AND c.column_name = col.name
        )
    ) THEN
        RETURN;
    END IF;
    EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I ON %I.%I (%s)',
        p_index, p_schema, p_table, p_columns
    );
END;
$$ LANGUAGE plpgsql;

SELECT jira_migration._create_index_if_columns_exist(
    'jira_project', 'security_level_members', 'idx_security_level_members_level', 'level_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_project', 'security_level_members', 'idx_security_level_members_member', 'member_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_project', 'security_levels', 'idx_security_levels_scheme', 'scheme_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_project', 'issue_security_schemes', 'idx_issue_security_schemes_project', 'project_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_project', 'project_role_members', 'idx_project_role_members_role', 'role_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_project', 'project_role_members', 'idx_project_role_members_project', 'project_id');

SELECT jira_migration._create_index_if_columns_exist(
    'jira_auth', 'user_groups', 'idx_user_groups_active', 'is_active');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_auth', 'user_group_memberships', 'idx_user_group_memberships_user', 'user_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_auth', 'user_group_memberships', 'idx_user_group_memberships_group', 'group_id');

SELECT jira_migration._create_index_if_columns_exist(
    'jira_admin', 'screen_tabs', 'idx_screen_tabs_screen', 'screen_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_sprint', 'board_configs', 'idx_board_configs_board', 'board_id');

SELECT jira_migration._create_index_if_columns_exist(
    'jira_plan', 'cumulative_flow_data', 'idx_cumulative_flow_data_board', 'board_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_plan', 'cumulative_flow_data', 'idx_cumulative_flow_data_date', 'data_date');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_plan', 'burndown_data', 'idx_burndown_data_sprint', 'sprint_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_plan', 'burndown_data', 'idx_burndown_data_date', 'data_date');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_plan', 'board_quick_filter_sharing', 'idx_board_quick_filter_sharing_filter', 'quick_filter_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_plan', 'lexorank_audit_log', 'idx_lexorank_audit_log_entity', 'entity_type, entity_id');
SELECT jira_migration._create_index_if_columns_exist(
    'jira_plan', 'sprint_goal_history', 'idx_sprint_goal_history_sprint', 'sprint_id');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'jira_comment' AND table_name = 'comments') THEN
        ALTER TABLE jira_comment.comments ADD COLUMN IF NOT EXISTS internal BOOLEAN DEFAULT FALSE;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'jira_attachment' AND table_name = 'attachments') THEN
        ALTER TABLE jira_attachment.attachments ADD COLUMN IF NOT EXISTS mime_type_detected VARCHAR(100);
        ALTER TABLE jira_attachment.attachments ADD COLUMN IF NOT EXISTS thumbnail_path VARCHAR(500);
    END IF;
END $$;

DROP FUNCTION IF EXISTS jira_migration._create_index_if_columns_exist(text, text, text, text);
