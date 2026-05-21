JIRA DATA CENTER ISSUE XML IMPORT ENGINE

Build a production-grade Jira Data Center Issue XML Import Engine with enterprise-scale architecture, deep XML compatibility, validation pipeline, attachment ingestion, field mapping, workflow linking, testing framework, rollback support, and import observability.

The system MUST achieve near 1:1 behavioral parity with real Jira Data Center XML imports.

You are NOT building a demo importer.
You are building an enterprise migration engine capable of importing real Jira Server/Data Center exports into a custom platform.

The importer MUST support:

Jira Data Center issue XML exports
Jira Server XML exports
Historical issue exports
Attachments
Comments
Worklogs
Changelog history
Custom fields
Labels
Components
Versions
Security levels
Watchers
Votes
Linked issues
Parent/subtask hierarchy
Rich text fields
Multi-select custom fields
Cascading select custom fields
Plugin-generated fields
Epic links
Sprint fields
Large attachment references
Broken reference handling
Cross-project migration

The engine MUST work with real exported XML from Jira DC.

CORE ARCHITECTURE

Build the engine with these modules:

XML INGESTION ENGINE
XML VALIDATION ENGINE
STAGING DATABASE
FIELD MAPPING ENGINE
ATTACHMENT PROCESSOR
ISSUE RELATIONSHIP ENGINE
CUSTOM FIELD RESOLUTION ENGINE
IMPORT EXECUTION ENGINE
AUDIT & LOGGING ENGINE
DRY RUN ENGINE
ROLLBACK ENGINE
IMPORT RESULT VISUALIZATION
TEST HARNESS
PERFORMANCE ENGINE
XML INGESTION ENGINE

Build a streaming XML parser.

MANDATORY:

SAX parser or equivalent
Never load full XML into memory
Support >10GB XML files
Support 1M+ issues

Must parse:

channel metadata
issue items
attachments
comments
customfields
worklogs
changelog histories
links
subtasks
labels
components
versions
security
priorities
statuses
users

Must preserve:

ordering
timestamps
hierarchy
source IDs
issue keys
XML VALIDATION ENGINE

Validate BEFORE import:

XML validation:

malformed XML
unsupported schema
invalid encoding
XXE attack protection
invalid entities
duplicate nodes

Issue validation:

duplicate issue keys
invalid references
missing statuses
missing priorities
orphaned subtasks
circular references
invalid attachment paths
missing users
corrupted dates

Field validation:

invalid custom field type
unsupported plugin fields
invalid enum values
invalid multi-select values
invalid numeric fields

Generate:

validation report
warnings
blockers
recoverable errors
import risk score
STAGING DATABASE

Create normalized staging tables:

staging_projects
staging_issues
staging_comments
staging_attachments
staging_custom_fields
staging_issue_links
staging_worklogs
staging_histories
staging_subtasks
staging_versions
staging_components
staging_users
staging_groups

Each record must contain:

source_id
source_key
import_batch_id
validation_state
checksum
raw_xml
parsed_payload
FIELD MAPPING ENGINE

Build intelligent field mapping.

Must support:

automatic mapping
manual remapping
plugin field handling
unknown field resolution
type conversion
rich text conversion
markdown conversion
enum resolution
status mapping
priority mapping

UI REQUIREMENT:
Build field mapping UI showing:

source field
target field
compatibility
validation errors
transformation preview
ATTACHMENT PROCESSOR

Must support:

binary attachment import
thumbnail references
large file streaming
chunked upload
retry mechanism
attachment deduplication
attachment validation
checksum verification

Handle:

missing files
corrupted binaries
unsupported mime types
duplicate filenames

Track:

attachment migration state
upload progress
checksum integrity
ISSUE RELATIONSHIP ENGINE

Must reconstruct:

parent-child hierarchy
epic-story mapping
issue links
blockers
relates-to
duplicates
clones

Handle:

broken references
cross-project references
orphaned issues

Must preserve:

source IDs
original relationships
CUSTOM FIELD RESOLUTION ENGINE

Support:

text fields
select lists
cascading selects
user pickers
group pickers
sprint fields
story points
epic links
plugin fields
date fields
multi-value fields

Need:

plugin compatibility layer
fallback renderer
unknown type registry
IMPORT EXECUTION ENGINE

Capabilities:

dry run
actual import
incremental import
resume failed imports
rollback support
retry support
queue-based execution
parallel workers

Must preserve:

timestamps
history
audit trail
source identifiers
AUDIT & LOGGING ENGINE

Track EVERYTHING.

Need:

import session logs
entity logs
attachment logs
transformation logs
validation logs
retry logs
rollback logs

Every imported entity must contain:

source reference
imported timestamp
imported by
migration batch ID
DRY RUN ENGINE

Simulate full import WITHOUT DB writes.

Need:

validation-only mode
estimated import time
estimated storage usage
issue counts
attachment counts
conflict detection
ROLLBACK ENGINE

Need:

transactional rollback
partial rollback
attachment cleanup
relationship cleanup
orphan cleanup

Rollback must restore system consistency.

IMPORT RESULT VISUALIZATION

Build enterprise import dashboard.

Must show:

imported issues
failed issues
skipped issues
attachment progress
warnings
field mismatches
import timeline
issue relationship graph
TEST HARNESS

Build automated tests.

Need:

corrupted XML tests
huge XML tests
malformed attachment tests
missing field tests
relationship integrity tests
performance tests
rollback tests
concurrency tests
PERFORMANCE REQUIREMENTS

Support:

1 million issues
100GB attachments
multi-threaded import
streaming architecture
resumable processing

Optimize:

indexing
queues
caching
batch commits
SECURITY REQUIREMENTS

Mandatory:

XXE prevention
upload validation
XML sanitization
path traversal prevention
attachment scanning
auth checks
audit trails
FINAL REQUIREMENT

The system MUST behave like a real Jira DC importer.

It must preserve:

issue fidelity
attachment integrity
history integrity
hierarchy integrity
workflow linkage
custom field semantics

The engine must be enterprise-ready and production-safe.

Use the provided Jira issue XML as the canonical fixture for development, parsing, validation, staging, testing, and import execution.