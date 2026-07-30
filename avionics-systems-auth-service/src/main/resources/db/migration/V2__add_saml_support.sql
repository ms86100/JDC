-- ============================================
-- SAML 2.0 SSO Support
-- ============================================

-- Allow SAML users who have no local password
ALTER TABLE jira_auth.users ALTER COLUMN password_hash DROP NOT NULL;

-- Track authentication provider and SAML identity
ALTER TABLE jira_auth.users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE jira_auth.users ADD COLUMN IF NOT EXISTS saml_name_id VARCHAR(255);
ALTER TABLE jira_auth.users ADD COLUMN IF NOT EXISTS saml_idp_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_users_saml_name_id ON jira_auth.users(saml_name_id);
CREATE INDEX IF NOT EXISTS idx_users_auth_provider ON jira_auth.users(auth_provider);

-- SAML IdP configurations (admin-managed)
CREATE TABLE IF NOT EXISTS jira_auth.saml_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    entity_id VARCHAR(500) NOT NULL,
    idp_entity_id VARCHAR(500) NOT NULL,
    idp_sso_url VARCHAR(1000) NOT NULL,
    idp_slo_url VARCHAR(1000),
    idp_certificate TEXT NOT NULL,
    sp_entity_id VARCHAR(500),
    acs_url VARCHAR(1000),
    attribute_mapping_email VARCHAR(200) DEFAULT 'email',
    attribute_mapping_username VARCHAR(200) DEFAULT 'username',
    attribute_mapping_display_name VARCHAR(200) DEFAULT 'displayName',
    attribute_mapping_groups VARCHAR(200) DEFAULT 'groups',
    default_role VARCHAR(50) DEFAULT 'ROLE_USER',
    auto_create_users BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    force_authn BOOLEAN NOT NULL DEFAULT FALSE,
    single_logout_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
