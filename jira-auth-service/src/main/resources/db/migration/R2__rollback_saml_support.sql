-- Rollback V2: SAML 2.0 SSO support
DROP TABLE IF EXISTS jira_auth.saml_configurations CASCADE;
DROP INDEX IF EXISTS jira_auth.idx_users_saml_name_id;
DROP INDEX IF EXISTS jira_auth.idx_users_auth_provider;
ALTER TABLE jira_auth.users DROP COLUMN IF EXISTS saml_idp_id;
ALTER TABLE jira_auth.users DROP COLUMN IF EXISTS saml_name_id;
ALTER TABLE jira_auth.users DROP COLUMN IF EXISTS auth_provider;
ALTER TABLE jira_auth.users ALTER COLUMN password_hash SET NOT NULL;
