CREATE TABLE IF NOT EXISTS jira_notification.incoming_mail_handlers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    server_type VARCHAR(10) NOT NULL DEFAULT 'IMAP',
    host VARCHAR(500) NOT NULL,
    port INTEGER NOT NULL DEFAULT 993,
    use_ssl BOOLEAN DEFAULT true,
    username VARCHAR(200) NOT NULL,
    encrypted_password TEXT NOT NULL,
    folder VARCHAR(100) DEFAULT 'INBOX',
    handler_type VARCHAR(30) NOT NULL DEFAULT 'CREATE_ISSUE',
    project_id UUID,
    issue_type_id UUID,
    default_reporter_id UUID,
    is_enabled BOOLEAN DEFAULT true,
    poll_interval_minutes INTEGER DEFAULT 5,
    last_poll_at TIMESTAMP,
    processed_message_ids TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mail_handlers_enabled ON jira_notification.incoming_mail_handlers(is_enabled);
