# Legacy Data Center XML sample (migration import)

The migration service supports two issue XML shapes (auto-detected):

1. **RSS 0.92** — `rss/channel/item` (canonical fixture: `.cursor/jira_dc_issue_export.xml`, test copy under `samples/jira_dc_issue_export.xml`)
2. **Entity backup** — `LegacyDcBackup/Entity/entityName/field` (see below)

Native Atlassian Legacy DC ZIP backup (`entities.xml`) is detected but **not yet imported**.

## Canonical test file

```
avionics-systems-migration-service/src/test/resources/samples/jira-dc-minimal-comment-attachment.xml
```

Contains: **Project** → **Issue** (`DEMO-1`) → **Comment** → **Attachment** (base64 `file` field).

## XML shape

```xml
<LegacyDcBackup>
  <Entity>
    <entityName>Comment</entityName>
    <field><name>issue</name><value>DEMO-1</value></field>
    <field><name>body</name><value>...</value></field>
  </Entity>
  <Entity>
    <entityName>Attachment</entityName>
    <field><name>issue</name><value>DEMO-1</value></field>
    <field><name>filename</name><value>file.txt</value></field>
    <field><name>file</name><value>BASE64_BYTES_HERE</value></field>
  </Entity>
</LegacyDcBackup>
```

## Validate parse only (no DB)

```powershell
cd avionics-systems-migration-service
mvn -q test -Dtest=LegacyDcXmlParserTest
```

## Run import (stub — no issue/attachment services required)

```powershell
.\scripts\run-jira-dc-import-sample.ps1 -StubDownstream
```

Uses `stubDownstream=true` so Comment/Attachment base64 is decoded and checksum-recorded locally.

## Run import (full stack)

Requires PostgreSQL + migration service (8094) + project/issue/comment/attachment services.

```powershell
.\scripts\run-jira-dc-import-sample.ps1
```

## API

```http
POST /api/migration/import/legacy-dc
Content-Type: multipart/form-data
X-User-Id: 00000000-0000-0000-0000-000000000001
X-Migration-Role: MIGRATION_OPERATOR

file=@jira-dc-minimal-comment-attachment.xml
options={"stubDownstream":true,"rollbackOnFailure":false}
```

## Atlassian DC / Confluence reference

Native Legacy Server/Data Center backup exports use proprietary XML inside the backup archive. This project normalizes imports through the **Entity + field** contract above. When exporting from a custom DC plugin or ETL, map columns to the field names in `LegacyDcEntityMapper`.
