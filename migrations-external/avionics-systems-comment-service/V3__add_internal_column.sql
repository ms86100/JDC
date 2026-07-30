-- V3: internal flag for customer-visible vs internal comments (Jira DC parity)
ALTER TABLE jira_comment.comments
    ADD COLUMN IF NOT EXISTS internal BOOLEAN DEFAULT FALSE NOT NULL;

COMMENT ON COLUMN jira_comment.comments.internal IS 'Internal comments hidden from customers';
