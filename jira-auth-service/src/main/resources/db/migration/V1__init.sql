-- Auth Service schema (public.users, public.roles, public.user_roles)

CREATE SCHEMA IF NOT EXISTS jira_auth;

CREATE TABLE IF NOT EXISTS jira_auth.users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_auth.roles (
    id UUID PRIMARY KEY,
    role_key VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS jira_auth.user_roles (
    user_id UUID NOT NULL REFERENCES jira_auth.users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES jira_auth.roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_users_username ON jira_auth.users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON jira_auth.users(email);
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON jira_auth.user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON jira_auth.user_roles(role_id);
