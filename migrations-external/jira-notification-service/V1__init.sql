-- Pure SQL UUID generator (works on PG 11+ without extensions)
CREATE OR REPLACE FUNCTION public.gen_random_uuid() RETURNS UUID AS $$
SELECT uuid_in(overlay(overlay(md5(random()::text || clock_timestamp()::text) placing '4' from 13) placing to_hex(floor(random()*(11-8+1) + 8)::int)::text from 17)::cstring)::uuid;
$$ LANGUAGE SQL VOLATILE;

-- V1__init.sql - Initial schema for jira-notification-service
-- Schema: jira_notification

-- Create extension for UUID generation if not exists

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS jira_notification;

-- Notifications table
CREATE TABLE jira_notification.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    reference_type VARCHAR(100),
    reference_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_notifications_user_id ON jira_notification.notifications(user_id);
CREATE INDEX idx_notifications_user_id_is_read ON jira_notification.notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON jira_notification.notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON jira_notification.notifications(reference_type, reference_id);

-- Notification preferences table
CREATE TABLE jira_notification.notification_preferences (
    user_id UUID NOT NULL,
    notification_type VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (user_id, notification_type)
);