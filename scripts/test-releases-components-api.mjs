/**
 * Rigorous CRUD smoke test for version + component services via gateway.
 *
 * Usage:
 *   node scripts/test-releases-components-api.mjs
 *   API_BASE_URL=http://localhost:8080 TEST_PROJECT_ID=<uuid> node scripts/test-releases-components-api.mjs
 */

const BASE = (process.env.API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const PROJECT_ID =
  process.env.TEST_PROJECT_ID || 'c90f43a2-fc32-4a88-a8c8-99f1bfa72565';

const passed = [];
const failed = [];

async function req(method, path, body) {
  const url = `${BASE}${path}`;
  const init = {
    method,
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
  };
  if (body !== undefined) init.body = JSON.stringify(body);
  const res = await fetch(url, init);
  let data = null;
  const text = await res.text();
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
  failed.push({ name, status });
  console.error(`FAIL ${name} (${status})`);
  return false;
}

async function main() {
  console.log(`Gateway: ${BASE}`);
  console.log(`Project: ${PROJECT_ID}\n`);

  const suffix = Date.now();
  let versionId;
  let versionId2;
  let componentId;

  // --- Versions ---
  let r = await req('GET', `/api/versions/project/${PROJECT_ID}`);
  ok('GET versions by project', r.status);

  r = await req('POST', '/api/versions', {
    projectId: PROJECT_ID,
    name: `E2E-Release-${suffix}`,
    description: 'API smoke test',
  });
  if (!ok('POST create version', r.status, [200, 201])) {
    console.error(r.data);
    return finish(1);
  }
  versionId = r.data?.id;
  if (!versionId) {
    failed.push({ name: 'version id present', status: 0 });
    return finish(1);
  }

  r = await req('GET', `/api/versions/${versionId}`);
  ok('GET version by id', r.status);

  r = await req('PUT', `/api/versions/${versionId}`, {
    name: `E2E-Release-${suffix}-updated`,
    description: 'Updated',
  });
  ok('PUT update version', r.status);

  r = await req('POST', `/api/versions/${versionId}/metrics/snapshot`);
  ok('POST metrics snapshot', r.status, [200, 201, 204]);

  r = await req('POST', `/api/versions/${versionId}/release-notes/generate`);
  ok('POST generate release notes', r.status, [200, 201, 204]);

  r = await req('POST', `/api/versions/${versionId}/release`, {});
  ok('POST release version', r.status, [200, 201]);

  r = await req('POST', `/api/versions/${versionId}/archive`);
  ok('POST archive version', r.status, [200, 201]);

  r = await req('POST', `/api/versions/${versionId}/unarchive`);
  ok('POST unarchive version', r.status, [200, 201]);

  // Second version for merge
  r = await req('POST', '/api/versions', {
    projectId: PROJECT_ID,
    name: `E2E-Merge-Source-${suffix}`,
  });
  if (ok('POST create merge source', r.status, [200, 201])) {
    versionId2 = r.data?.id;
    if (versionId2) {
      r = await req('POST', '/api/versions/merge', {
        sourceVersionId: versionId2,
        targetVersionId: versionId,
      });
      ok('POST merge versions', r.status, [200, 201]);
    }
  }

  r = await req('DELETE', `/api/versions/${versionId}`);
  ok('DELETE version (target after merge)', r.status, [200, 204]);

  // --- Components ---
  r = await req('GET', `/api/components/project/${PROJECT_ID}`);
  ok('GET components by project', r.status);

  r = await req('POST', '/api/components', {
    projectId: PROJECT_ID,
    name: `E2E-Component-${suffix}`,
    description: 'API smoke test',
    assigneeType: 'PROJECT_DEFAULT',
  });
  if (!ok('POST create component', r.status, [200, 201])) {
    console.error(r.data);
    return finish(1);
  }
  componentId = r.data?.id;

  r = await req('GET', `/api/components/${componentId}`);
  ok('GET component by id', r.status);

  r = await req('PUT', `/api/components/${componentId}`, {
    name: `E2E-Component-${suffix}-updated`,
    description: 'Updated',
    assigneeType: 'PROJECT_DEFAULT',
  });
  ok('PUT update component', r.status);

  r = await req('POST', `/api/components/${componentId}/archive`);
  ok('POST archive component', r.status, [200, 201]);

  r = await req('POST', `/api/components/${componentId}/unarchive`);
  ok('POST unarchive component', r.status, [200, 201]);

  r = await req('GET', `/api/components/${componentId}/audit`);
  ok('GET component audit', r.status, [200, 201]);

  r = await req('DELETE', `/api/components/${componentId}`);
  ok('DELETE component', r.status, [200, 204]);

  return finish(failed.length ? 1 : 0);
}

function finish(code) {
  console.log(`\n--- Summary: ${passed.length} passed, ${failed.length} failed ---`);
  if (failed.length) {
    console.log('Failures:', failed);
  }
  process.exit(code);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
