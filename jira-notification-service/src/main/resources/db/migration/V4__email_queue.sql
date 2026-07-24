CREATE TABLE IF NOT EXISTS jira_notification.email_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(300) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_html TEXT,
    status VARCHAR(20) DEFAULT 'QUEUED',
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    created_at TIMESTAMP DEFAULT NOW(),
    sent_at TIMESTAMP,
    next_retry_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_queue_status ON jira_notification.email_queue(status);
CREATE INDEX IF NOT EXISTS idx_email_queue_next_retry ON jira_notification.email_queue(next_retry_at);
