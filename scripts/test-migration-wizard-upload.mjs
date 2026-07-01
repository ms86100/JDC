/**
 * CSV migration wizard smoke test (create session → upload → preview → validate).
 *
 * Usage:
 *   node scripts/test-migration-wizard-upload.mjs
 *   API_BASE_URL=http://localhost:8080 TEST_PROJECT_ID=<uuid> node scripts/test-migration-wizard-upload.mjs
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const BASE = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const PROJECT_ID =
  process.env.TEST_PROJECT_ID || 'c90f43a2-fc32-4a88-a8c8-99f1bfa72565';
const USER_ID = process.env.MIGRATION_USER_ID || '00000000-0000-0000-0000-000000000001';
const ROLE = process.env.MIGRATION_ROLE || 'MIGRATION_OPERATOR';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const SAMPLE_CSV = `Summary,Issue Type,Issue key,Priority,Status,Project key,Description
Smoke test issue,Task,SMOKE-1,Medium,To Do,PROJ,First row
Second issue,Story,SMOKE-2,High,In Progress,PROJ,Second row
Third issue,Bug,SMOKE-3,Low,Done,PROJ,Third row
`;

const passed = [];
const failed = [];

function headers(json = true) {
  const h = {
    'X-User-Id': USER_ID,
    'X-Migration-Role': ROLE,
  };
  if (json) h['Content-Type'] = 'application/json';
  return h;
}

async function req(method, urlPath, body, isForm = false) {
  const url = `${BASE}${urlPath}`;
  const init = { method, headers: isForm ? headers(false) : headers() };
  if (body !== undefined) init.body = body;
  const res = await fetch(url, init);
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }
  return { status: res.status, data };
}

function ok(name, status, allowed = [200, 201, 204]) {
  if (allowed.includes(status)) {
    passed.push({ name, status });
    console.log(`PASS ${name} (${status})`);
    return true;
  }
  failed.push({ name, status, data: arguments[3] });
  console.error(`FAIL ${name} (${status})`, arguments[3] ?? '');
  return false;
}

async function main() {
  console.log(`Gateway: ${BASE}`);
  console.log(`Project: ${PROJECT_ID}\n`);

  let r = await req('GET', '/api/migration/health/cluster');
  if (!ok('GET migration cluster health', r.status)) {
    console.error('Start jira-migration-service on port 8094 and gateway on 8080.');
    return finish(1);
  }

  r = await req('POST', '/api/migration/wizard/sessions', JSON.stringify({
    importType: 'CSV',
    targetProjectId: PROJECT_ID,
  }));
  if (!ok('POST create wizard session', r.status, [200, 201])) return finish(1);
  const sessionId = r.data?.sessionId;
  if (!sessionId) {
    console.error('No sessionId in response');
    return finish(1);
  }

  const tmpFile = path.join(__dirname, 'fixtures', 'wizard-smoke.csv');
  fs.mkdirSync(path.dirname(tmpFile), { recursive: true });
  fs.writeFileSync(tmpFile, SAMPLE_CSV, 'utf8');

  const boundary = '----MigrationWizard' + Date.now();
  const fileBuf = fs.readFileSync(tmpFile);
  const formBody = [
    `--${boundary}`,
    'Content-Disposition: form-data; name="file"; filename="wizard-smoke.csv"',
    'Content-Type: text/csv',
    '',
    fileBuf.toString('utf8'),
    `--${boundary}`,
    'Content-Disposition: form-data; name="importType"',
    '',
    'CSV',
    `--${boundary}--`,
    '',
  ].join('\r\n');

  const uploadRes = await fetch(`${BASE}/api/migration/wizard/sessions/${sessionId}/upload`, {
    method: 'POST',
    headers: {
      'X-User-Id': USER_ID,
      'X-Migration-Role': ROLE,
      'Content-Type': `multipart/form-data; boundary=${boundary}`,
    },
    body: formBody,
  });
  const uploadText = await uploadRes.text();
  let uploadData = null;
  try {
    uploadData = JSON.parse(uploadText);
  } catch {
    uploadData = uploadText;
  }
  r = { status: uploadRes.status, data: uploadData };

  if (!ok('POST upload CSV', r.status, [200, 201])) return finish(1);
  if (!r.data?.success) {
    console.error('Upload returned success=false:', r.data?.errorMessage);
    return finish(1);
  }
  if ((r.data?.totalRows ?? 0) < 1) {
    console.error('Upload parsed 0 rows:', r.data);
    return finish(1);
  }
  console.log(`  Parsed ${r.data.totalRows} rows, headers: ${(r.data.detectedHeaders || []).length}`);

  r = await req('GET', `/api/migration/wizard/sessions/${sessionId}/preview?page=0&size=10`);
  if (!ok('GET preview', r.status)) return finish(1);

  r = await req(
    'POST',
    `/api/migration/wizard/sessions/${sessionId}/validate?entityType=ISSUE`,
    null,
  );
  if (!ok('POST validate session', r.status, [200, 400])) return finish(1);

  r = await req('PATCH', `/api/migration/wizard/sessions/${sessionId}`, JSON.stringify({
    step: 'TARGET_PROJECT',
    targetProjectId: PROJECT_ID,
  }));
  ok('PATCH update session target', r.status, [200]);

  return finish(failed.length ? 1 : 0);
}

function finish(code) {
  console.log(`\n--- Summary: ${passed.length} passed, ${failed.length} failed ---`);
  process.exit(code);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
