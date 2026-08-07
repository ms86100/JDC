-- V15: Add VVO Guideline fields to vvo_definition
-- Fields: pts_mfcl_links, n_di, reference_documents, dts_baseline_version, baseline_verified

SET search_path TO jira_test;

ALTER TABLE vvo_definition ADD COLUMN IF NOT EXISTS pts_mfcl_links TEXT[];
ALTER TABLE vvo_definition ADD COLUMN IF NOT EXISTS n_di TEXT[];
ALTER TABLE vvo_definition ADD COLUMN IF NOT EXISTS reference_documents TEXT;
ALTER TABLE vvo_definition ADD COLUMN IF NOT EXISTS dts_baseline_version TEXT;
ALTER TABLE vvo_definition ADD COLUMN IF NOT EXISTS baseline_verified BOOLEAN DEFAULT TRUE;
