-- Comprehensive User Management Seed Data
-- Matches Jira Data Center patterns: users, groups, memberships, application access, login info
-- Also syncs users to jira_auth schema for login capability
-- Run via: docker exec -i jira-postgres psql -U jiraadmin -d jira_platform < postgres/seed-user-management.sql

BEGIN;

-- ============================================================
-- 1. ADDITIONAL USERS (8 new users beyond admin and ms86100)
-- ============================================================

INSERT INTO jira_admin.cwd_user
    (id, directory_id, user_name, password_hash, email_address, display_name, active, first_name, last_name, lower_user_name)
VALUES
    ('a0000000-0000-0000-0000-000000000201', '00000000-0000-0000-0000-000000000001',
     'john.smith', '$2b$10$hashedpassword', 'john.smith@example.com', 'John Smith',
     true, 'John', 'Smith', 'john.smith'),

    ('a0000000-0000-0000-0000-000000000202', '00000000-0000-0000-0000-000000000001',
     'jane.doe', '$2b$10$hashedpassword', 'jane.doe@example.com', 'Jane Doe',
     true, 'Jane', 'Doe', 'jane.doe'),

    ('a0000000-0000-0000-0000-000000000203', '00000000-0000-0000-0000-000000000001',
     'bob.wilson', '$2b$10$hashedpassword', 'bob.wilson@example.com', 'Bob Wilson',
     true, 'Bob', 'Wilson', 'bob.wilson'),

    ('a0000000-0000-0000-0000-000000000204', '00000000-0000-0000-0000-000000000001',
     'alice.johnson', '$2b$10$hashedpassword', 'alice.johnson@example.com', 'Alice Johnson',
     true, 'Alice', 'Johnson', 'alice.johnson'),

    ('a0000000-0000-0000-0000-000000000205', '00000000-0000-0000-0000-000000000001',
     'charlie.brown', '$2b$10$hashedpassword', 'charlie.brown@example.com', 'Charlie Brown',
     true, 'Charlie', 'Brown', 'charlie.brown'),

    ('a0000000-0000-0000-0000-000000000206', '00000000-0000-0000-0000-000000000001',
     'diana.prince', '$2b$10$hashedpassword', 'diana.prince@example.com', 'Diana Prince',
     true, 'Diana', 'Prince', 'diana.prince'),

    ('a0000000-0000-0000-0000-000000000207', '00000000-0000-0000-0000-000000000001',
     'eve.williams', '$2b$10$hashedpassword', 'eve.williams@example.com', 'Eve Williams',
     true, 'Eve', 'Williams', 'eve.williams'),

    ('a0000000-0000-0000-0000-000000000208', '00000000-0000-0000-0000-000000000001',
     'frank.miller', '$2b$10$hashedpassword', 'frank.miller@example.com', 'Frank Miller',
     true, 'Frank', 'Miller', 'frank.miller')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 2. CUSTOM GROUPS (4 new groups beyond system groups)
-- ============================================================

INSERT INTO jira_admin.cwd_group
    (id, directory_id, group_name, description, active, lower_group_name, is_global, is_system)
VALUES
    ('b0000000-0000-0000-0000-000000000020', '00000000-0000-0000-0000-000000000001',
     'team-backend', 'Backend development team', true, 'team-backend', false, false),

    ('b0000000-0000-0000-0000-000000000021', '00000000-0000-0000-0000-000000000001',
     'team-frontend', 'Frontend development team', true, 'team-frontend', false, false),

    ('b0000000-0000-0000-0000-000000000022', '00000000-0000-0000-0000-000000000001',
     'qa-team', 'Quality assurance team', true, 'qa-team', false, false),

    ('b0000000-0000-0000-0000-000000000023', '00000000-0000-0000-0000-000000000001',
     'project-managers', 'Project managers', true, 'project-managers', false, false)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. GROUP MEMBERSHIPS
-- ============================================================

-- All new users → jira-software-users (everyone gets base app access)
INSERT INTO jira_admin.cwd_membership (parent_id, child_id, membership_type) VALUES
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000201', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000202', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000203', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000204', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000205', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000206', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000207', 'GROUP_USER'),
    ('00000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000208', 'GROUP_USER')
ON CONFLICT DO NOTHING;

-- team-backend: john.smith, charlie.brown, frank.miller
INSERT INTO jira_admin.cwd_membership (parent_id, child_id, membership_type) VALUES
    ('b0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000201', 'GROUP_USER'),
    ('b0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000205', 'GROUP_USER'),
    ('b0000000-0000-0000-0000-000000000020', 'a0000000-0000-0000-0000-000000000208', 'GROUP_USER')
ON CONFLICT DO NOTHING;

-- team-frontend: jane.doe, eve.williams
INSERT INTO jira_admin.cwd_membership (parent_id, child_id, membership_type) VALUES
    ('b0000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000202', 'GROUP_USER'),
    ('b0000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000207', 'GROUP_USER')
ON CONFLICT DO NOTHING;

-- qa-team: bob.wilson, diana.prince
INSERT INTO jira_admin.cwd_membership (parent_id, child_id, membership_type) VALUES
    ('b0000000-0000-0000-0000-000000000022', 'a0000000-0000-0000-0000-000000000203', 'GROUP_USER'),
    ('b0000000-0000-0000-0000-000000000022', 'a0000000-0000-0000-0000-000000000206', 'GROUP_USER')
ON CONFLICT DO NOTHING;

-- project-managers: alice.johnson, ms86100
INSERT INTO jira_admin.cwd_membership (parent_id, child_id, membership_type) VALUES
    ('b0000000-0000-0000-0000-000000000023', 'a0000000-0000-0000-0000-000000000204', 'GROUP_USER'),
    ('b0000000-0000-0000-0000-000000000023', '90b5c96e-150e-495c-af46-1e8a1c952647', 'GROUP_USER')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. APPLICATION ACCESS (jira-software for all new users)
-- ============================================================

INSERT INTO jira_admin.application_access (user_id, application_key, active) VALUES
    ('a0000000-0000-0000-0000-000000000201', 'jira-software', true),
    ('a0000000-0000-0000-0000-000000000202', 'jira-software', true),
    ('a0000000-0000-0000-0000-000000000203', 'jira-software', true),
    ('a0000000-0000-0000-0000-000000000204', 'jira-software', true),
    ('a0000000-0000-0000-0000-000000000205', 'jira-software', true),
    ('a0000000-0000-0000-0000-000000000206', 'jira-software', true),
    ('a0000000-0000-0000-0000-000000000207', 'jira-software', true),
    ('a0000000-0000-0000-0000-000000000208', 'jira-software', true)
ON CONFLICT DO NOTHING;

-- ============================================================
-- 5. LOGIN INFO (realistic login counts)
-- ============================================================

INSERT INTO jira_admin.login_info (user_id, login_count, last_login_date) VALUES
    ('a0000000-0000-0000-0000-000000000201', 42, NOW() - INTERVAL '2 hours'),
    ('a0000000-0000-0000-0000-000000000202', 38, NOW() - INTERVAL '1 day'),
    ('a0000000-0000-0000-0000-000000000203', 15, NOW() - INTERVAL '3 hours'),
    ('a0000000-0000-0000-0000-000000000204', 67, NOW() - INTERVAL '30 minutes'),
    ('a0000000-0000-0000-0000-000000000205', 28, NOW() - INTERVAL '5 hours'),
    ('a0000000-0000-0000-0000-000000000206', 12, NOW() - INTERVAL '2 days'),
    ('a0000000-0000-0000-0000-000000000207', 31, NOW() - INTERVAL '4 hours'),
    ('a0000000-0000-0000-0000-000000000208', 19, NOW() - INTERVAL '1 hour')
ON CONFLICT DO NOTHING;

-- ============================================================
-- 6. AUTH SERVICE SYNC (so users can actually log in)
--    Password for all seed users: "password123"
--    BCrypt(12) hash pre-computed
-- ============================================================

-- Ensure roles exist
INSERT INTO jira_auth.roles (id, role_key, name, description)
VALUES
    ('00000000-0000-0000-0000-aaaaaaaaa001', 'ROLE_ADMIN', 'ROLE_ADMIN', 'System ADMIN'),
    ('00000000-0000-0000-0000-aaaaaaaaa002', 'ROLE_USER', 'ROLE_USER', 'System USER')
ON CONFLICT (role_key) DO NOTHING;

-- Insert users with BCrypt(12) hash for "password123"
INSERT INTO jira_auth.users (id, username, email, password_hash, active) VALUES
    ('a0000000-0000-0000-0000-000000000201', 'john.smith', 'john.smith@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true),
    ('a0000000-0000-0000-0000-000000000202', 'jane.doe', 'jane.doe@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true),
    ('a0000000-0000-0000-0000-000000000203', 'bob.wilson', 'bob.wilson@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true),
    ('a0000000-0000-0000-0000-000000000204', 'alice.johnson', 'alice.johnson@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true),
    ('a0000000-0000-0000-0000-000000000205', 'charlie.brown', 'charlie.brown@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true),
    ('a0000000-0000-0000-0000-000000000206', 'diana.prince', 'diana.prince@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true),
    ('a0000000-0000-0000-0000-000000000207', 'eve.williams', 'eve.williams@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true),
    ('a0000000-0000-0000-0000-000000000208', 'frank.miller', 'frank.miller@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true)
ON CONFLICT (username) DO NOTHING;

-- Assign ROLE_USER to all seed users
INSERT INTO jira_auth.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM jira_auth.users u
CROSS JOIN jira_auth.roles r
WHERE u.username IN ('john.smith','jane.doe','bob.wilson','alice.johnson','charlie.brown','diana.prince','eve.williams','frank.miller')
  AND r.role_key = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- Also ensure existing admin/ms86100 are in auth if missing
INSERT INTO jira_auth.users (id, username, email, password_hash, active) VALUES
    ('00000000-0000-0000-0000-000000000100', 'admin', 'admin@example.com',
     '$2a$12$LJ3m4ys3uz0LKlhJLk6rg.vhIK4BYYFKBaFJdV2w9RAE9Y0LNmi7C', true)
ON CONFLICT (username) DO NOTHING;

COMMIT;

-- Verification queries
SELECT 'Users: ' || count(*) FROM jira_admin.cwd_user;
SELECT 'Groups: ' || count(*) FROM jira_admin.cwd_group;
SELECT 'Memberships: ' || count(*) FROM jira_admin.cwd_membership;
SELECT 'Auth Users: ' || count(*) FROM jira_auth.users;
