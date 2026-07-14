import fs from 'fs';

// Exact copy of parseCsvContent from useValidation.ts
function parseCsvContent(content) {
  const rows = [];
  let current = '';
  let inQuotes = false;
  let row = [];

  for (let i = 0; i < content.length; i++) {
    const char = content[i];
    const nextChar = content[i + 1];

    if (char === '"') {
      if (inQuotes && nextChar === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === ',' && !inQuotes) {
      row.push(current.trim());
      current = '';
    } else if ((char === '\r' || char === '\n') && !inQuotes) {
      if (char === '\r' && nextChar === '\n') i++;
      row.push(current.trim());
      current = '';
      if (row.some((cell) => cell !== '')) {
        rows.push(row);
      }
      row = [];
    } else {
      current += char;
    }
  }

  row.push(current.trim());
  if (row.some((cell) => cell !== '')) {
    rows.push(row);
  }

  return rows;
}

// Exact copy of validateCsvClientSide logic
function validateCsv(filePath) {
  console.log(`\n========== Testing: ${filePath} ==========`);
  let content = fs.readFileSync(filePath, 'utf-8');
  if (content.charCodeAt(0) === 0xfeff) {
    content = content.slice(1);
  }

  const allRows = parseCsvContent(content);
  console.log(`Total parsed rows: ${allRows.length}`);

  if (allRows.length === 0) {
    console.log('ERROR: File is empty');
    return;
  }

  const headers = allRows[0];
  console.log(`Headers count: ${headers.length}`);
  console.log(`Headers: ${headers.slice(0, 5).join(', ')}...`);

  const dataRows = allRows.slice(1);
  console.log(`Data rows: ${dataRows.length}`);

  // Check column count mismatches
  for (let i = 0; i < dataRows.length; i++) {
    const row = dataRows[i];
    if (row.length !== headers.length) {
      console.log(`ROW ${i + 2}: column mismatch - has ${row.length} columns, expected ${headers.length}`);
      if (row.length < headers.length) {
        console.log(`  Missing columns: ${headers.length - row.length}`);
      } else {
        console.log(`  Extra columns: ${row.length - headers.length}`);
      }
    }
  }

  // Now simulate the EXACT validation code that might throw .trim() error
  const previewRows = dataRows.slice(0, 10);
  console.log(`\nSimulating validateCsvClientSide on ${previewRows.length} preview rows...`);

  try {
    // Check for empty headers  (line 139-150)
    headers.forEach((header, index) => {
      if (!header.trim()) {
        console.log(`  Empty header at column ${index + 1}`);
      }
    });

    // Check for duplicate headers (line 153-167)
    const headerSet = new Set();
    headers.forEach((header) => {
      const normalized = header.toLowerCase().trim();
      if (headerSet.has(normalized)) {
        console.log(`  Duplicate header: ${header}`);
      }
      headerSet.add(normalized);
    });

    // Validate data rows (line 174-252)
    previewRows.forEach((row, rowIndex) => {
      // Check column count mismatch
      if (row.length !== headers.length) {
        // This is just a warning, not crash
      }

      // Check for empty rows (line 189)
      if (row.every((cell) => !(cell ?? '').trim())) {
        console.log(`  Row ${rowIndex + 2} is empty`);
      }

      // Check required fields (line 202-220)
      const requiredFields = ['summary', 'issuetype', 'project'];
      requiredFields.forEach((field) => {
        const fieldIndex = headers.findIndex(
          (h) => h.toLowerCase().replace(/[\s_-]/g, '') === field
        );
        if (fieldIndex !== -1) {
          const value = row[fieldIndex] ?? '';
          if (!value.trim()) {
            // warning only
          }
        }
      });

      // Check for invalid characters (line 222-252)
      row.forEach((cellRaw, colIndex) => {
        const cell = cellRaw ?? '';
        const quoteCount = (cell.match(/"/g) || []).length;
        if (quoteCount % 2 !== 0) {
          console.log(`  Row ${rowIndex + 2}, col ${colIndex}: unclosed quote`);
        }
        const headerLower = (headers[colIndex] ?? '').toLowerCase().replace(/[\s_-]/g, '');
        if (headerLower === 'issuekey' || headerLower === 'key') {
          if (!/^[A-Z][A-Z0-9]*-[0-9]+$/i.test(cell) && cell.trim()) {
            // warning
          }
        }
      });
    });

    console.log('Client-side validation completed WITHOUT errors.\n');
  } catch (e) {
    console.log(`CRASH: ${e.message}`);
    console.log(e.stack);
  }

  // Now simulate generateFieldMappings (line 307-331)
  console.log('Simulating generateFieldMappings...');
  try {
    const targetFieldKeys = ['summary', 'description', 'issuetype', 'issueKey', 'priority', 'project', 'status', 'assignee', 'reporter', 'labels', 'parent', 'epic'];

    // matchHeaderToTargetField logic
    function normalizeMigrationHeader(header) {
      if (!header) return '';
      return header.trim().toLowerCase().replace(/[\s-]+/g, '_').replace(/[^a-z0-9_]/g, '');
    }

    const JIRA_CSV_ALIASES = {
      summary: ['summary', 'title', 'subject'],
      description: ['description', 'desc', 'body', 'details', 'content'],
      issuetype: ['issuetype', 'issue_type', 'type'],
      issueKey: ['issuekey', 'issue_key', 'key'],
      priority: ['priority', 'prio'],
      project: ['project', 'proj', 'project_key', 'projectkey'],
      project_key: ['project_key', 'projectkey'],
      status: ['status', 'state', 'workflow_status'],
      assignee: ['assignee', 'assigned_to', 'assignedto'],
      reporter: ['reporter', 'reported_by', 'reportedby'],
    };

    function matchHeaderToTargetField(header, targetFieldKeys) {
      const normalized = normalizeMigrationHeader(header);
      if (!normalized) return null;
      for (const [targetField, aliases] of Object.entries(JIRA_CSV_ALIASES)) {
        if (aliases.some((alias) => alias === normalized)) return targetField;
      }
      for (const field of targetFieldKeys) {
        const normField = normalizeMigrationHeader(field);
        if (normField && normField === normalized) return field;
      }
      return null;
    }

    headers.forEach((header) => {
      const matchedField = matchHeaderToTargetField(header, targetFieldKeys) ?? '';
      // This should never crash
    });

    console.log('generateFieldMappings completed WITHOUT errors.\n');
  } catch (e) {
    console.log(`CRASH in generateFieldMappings: ${e.message}`);
    console.log(e.stack);
  }
}

// Test both CSVs
const csv1 = process.argv[2] || 'C:\\Users\\SSHABNSA\\Downloads\\issue - issue.csv';
const csv2 = process.argv[3] || 'C:\\Users\\SSHABNSA\\Downloads\\Jira 2026-07-13T11_42_56+0200.csv';

if (fs.existsSync(csv1)) validateCsv(csv1);
if (fs.existsSync(csv2)) validateCsv(csv2);
