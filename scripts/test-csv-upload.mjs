import fs from 'fs';
import http from 'http';
import path from 'path';

const GATEWAY = process.env.API_BASE_URL || 'http://localhost:8080';
const USER_ID = '00000000-0000-0000-0000-000000000001';
const ROLE = 'MIGRATION_ADMIN';

function request(method, urlPath, body, extraHeaders = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlPath, GATEWAY);
    const opts = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method,
      headers: {
        'X-User-Id': USER_ID,
        'X-Migration-Role': ROLE,
        ...extraHeaders,
      },
    };
    const req = http.request(opts, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const raw = Buffer.concat(chunks).toString();
        try { resolve({ status: res.statusCode, data: JSON.parse(raw) }); }
        catch { resolve({ status: res.statusCode, data: raw }); }
      });
    });
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

function multipartUpload(urlPath, filePath) {
  return new Promise((resolve, reject) => {
    const boundary = '----FormBoundary' + Date.now();
    const fileName = path.basename(filePath);
    const fileContent = fs.readFileSync(filePath);

    const fileHeader = `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: text/csv\r\n\r\n`;
    const fileTail = `\r\n--${boundary}--\r\n`;

    const bodyBuf = Buffer.concat([
      Buffer.from(fileHeader),
      fileContent,
      Buffer.from(fileTail),
    ]);

    const url = new URL(urlPath, GATEWAY);
    const opts = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method: 'POST',
      headers: {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'Content-Length': bodyBuf.length,
        'X-User-Id': USER_ID,
        'X-Migration-Role': ROLE,
      },
    };
    const req = http.request(opts, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => {
        const raw = Buffer.concat(chunks).toString();
        try { resolve({ status: res.statusCode, data: JSON.parse(raw) }); }
        catch { resolve({ status: res.statusCode, data: raw }); }
      });
    });
    req.on('error', reject);
    req.write(bodyBuf);
    req.end();
  });
}

async function testCsv(csvFile) {
  console.log(`\n${'='.repeat(60)}`);
  console.log(`Testing CSV: ${csvFile}`);
  console.log(`${'='.repeat(60)}`);

  // 1. Create wizard session
  console.log('\n1. Creating wizard session...');
  const session = await request('POST', '/api/migration/wizard/sessions',
    JSON.stringify({ importType: 'CSV' }),
    { 'Content-Type': 'application/json' }
  );
  console.log(`   Status: ${session.status}`);
  if (session.status >= 400) {
    console.log('   FAILED:', JSON.stringify(session.data).substring(0, 500));
    return;
  }
  const sessionId = session.data.sessionId;
  console.log(`   Session ID: ${sessionId}`);

  // 2. Upload CSV
  console.log('\n2. Uploading CSV...');
  const upload = await multipartUpload(
    `/api/migration/wizard/sessions/${sessionId}/upload?importType=CSV`,
    csvFile
  );
  console.log(`   Status: ${upload.status}`);
  if (upload.status >= 400) {
    console.log('   FAILED:', JSON.stringify(upload.data).substring(0, 1000));
    return;
  }
  console.log(`   Success: ${upload.data?.success}`);
  console.log(`   Error: ${upload.data?.errorMessage || 'none'}`);
  console.log(`   Total rows: ${upload.data?.totalRows}`);
  console.log(`   Detected entity type: ${upload.data?.detectedEntityType}`);
  console.log(`   Detected headers count: ${upload.data?.detectedHeaders?.length}`);

  if (upload.data?.detectedHeaders) {
    console.log(`   Headers: [${upload.data.detectedHeaders.join(', ')}]`);
    const nullHeaders = upload.data.detectedHeaders.map((h, i) => h == null ? i : -1).filter(i => i >= 0);
    if (nullHeaders.length > 0) {
      console.log(`   *** NULL HEADERS at indices: ${nullHeaders.join(', ')} ***`);
    }
  }

  // 3. Validate
  console.log('\n3. Validating session...');
  const validate = await request('POST',
    `/api/migration/wizard/sessions/${sessionId}/validate?entityType=ISSUE`);
  console.log(`   Status: ${validate.status}`);
  if (validate.status >= 400) {
    console.log('   FAILED:', JSON.stringify(validate.data).substring(0, 1000));
    return;
  }
  console.log(`   Valid: ${validate.data?.valid}`);
  console.log(`   Errors: ${validate.data?.errors?.length || 0}`);
  console.log(`   Warnings: ${validate.data?.warnings?.length || 0}`);

  if (validate.data?.errors?.length > 0) {
    const byCode = {};
    validate.data.errors.forEach(e => { byCode[e.errorCode || e.code] = (byCode[e.errorCode || e.code] || 0) + 1; });
    console.log('   Error summary:', JSON.stringify(byCode));
    console.log('   First 3 errors:');
    validate.data.errors.slice(0, 3).forEach((e, i) => {
      console.log(`     ${i + 1}. Row ${e.row}: [${e.field}] ${e.message} (${e.errorCode})`);
    });
  }
}

const csv1 = 'C:\\Users\\SSHABNSA\\Downloads\\issue - issue.csv';
const csv2 = 'C:\\Users\\SSHABNSA\\Downloads\\Jira 2026-07-13T11_42_56+0200.csv';

(async () => {
  if (fs.existsSync(csv1)) await testCsv(csv1);
  if (fs.existsSync(csv2)) await testCsv(csv2);
})().catch(e => console.error('FATAL:', e));
