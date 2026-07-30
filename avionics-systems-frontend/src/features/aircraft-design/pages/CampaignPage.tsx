import { useState, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { vvoApi } from '../../../api/vvoApi';
import '../AircraftDesignStyles.css';

interface ParsedVvo {
  row: number;
  summary: string;
  description?: string;
  vvoType?: string;
  testMean?: string;
  system?: string;
  applicability?: string;
  valid: boolean;
  error?: string;
}

interface CampaignLog {
  timestamp: string;
  message: string;
  type: 'info' | 'success' | 'error';
}

export default function CampaignPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [projectInput, setProjectInput] = useState(projectId);
  const [fileName, setFileName] = useState('');
  const [csvContent, setCsvContent] = useState('');
  const [parsedVvos, setParsedVvos] = useState<ParsedVvo[]>([]);
  const [creating, setCreating] = useState(false);
  const [logs, setLogs] = useState<CampaignLog[]>([]);
  const [completed, setCompleted] = useState(false);

  function addLog(message: string, type: 'info' | 'success' | 'error' = 'info') {
    setLogs(prev => [...prev, { timestamp: new Date().toLocaleTimeString(), message, type }]);
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    setCompleted(false);
    setLogs([]);

    const reader = new FileReader();
    reader.onload = (ev) => {
      const content = ev.target?.result as string;
      setCsvContent(content);
      parseCsv(content);
    };
    reader.readAsText(file);
  }

  function parseCsv(content: string) {
    const lines = content.split('\n').map(l => l.trim()).filter(l => l.length > 0);
    if (lines.length < 2) {
      addLog('CSV must have a header row and at least one data row', 'error');
      setParsedVvos([]);
      return;
    }

    const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
    const summaryIdx = headers.findIndex(h => h === 'summary' || h === 'title' || h === 'name');
    const descIdx = headers.findIndex(h => h === 'description' || h === 'desc');
    const typeIdx = headers.findIndex(h => h === 'vvotype' || h === 'type' || h === 'vvo_type');
    const testMeanIdx = headers.findIndex(h => h === 'testmean' || h === 'test_mean' || h === 'testmeans');
    const systemIdx = headers.findIndex(h => h === 'system' || h === 'systemname');
    const applicabilityIdx = headers.findIndex(h => h === 'applicability' || h === 'aircraft');

    if (summaryIdx < 0) {
      addLog('CSV must have a "summary" or "title" column', 'error');
      setParsedVvos([]);
      return;
    }

    const parsed: ParsedVvo[] = [];
    for (let i = 1; i < lines.length; i++) {
      const cols = lines[i].split(',').map(c => c.trim());
      const summary = cols[summaryIdx] || '';
      const vvo: ParsedVvo = {
        row: i,
        summary,
        description: descIdx >= 0 ? cols[descIdx] : undefined,
        vvoType: typeIdx >= 0 ? cols[typeIdx] : undefined,
        testMean: testMeanIdx >= 0 ? cols[testMeanIdx] : undefined,
        system: systemIdx >= 0 ? cols[systemIdx] : undefined,
        applicability: applicabilityIdx >= 0 ? cols[applicabilityIdx] : undefined,
        valid: summary.length > 0,
        error: summary.length === 0 ? 'Missing summary' : undefined,
      };
      parsed.push(vvo);
    }

    setParsedVvos(parsed);
    addLog(`Parsed ${parsed.length} rows, ${parsed.filter(v => v.valid).length} valid`, 'info');
  }

  async function handleCreateCampaign() {
    const validVvos = parsedVvos.filter(v => v.valid);
    if (validVvos.length === 0) {
      addLog('No valid VVOs to create', 'error');
      return;
    }
    if (!projectInput) {
      addLog('Project ID is required', 'error');
      return;
    }

    setCreating(true);
    setCompleted(false);
    addLog(`Starting campaign: creating ${validVvos.length} VVOs...`, 'info');

    let successCount = 0;
    let failCount = 0;

    for (const vvo of validVvos) {
      try {
        await vvoApi.create({
          projectId: projectInput,
          summary: vvo.summary,
          description: vvo.description,
          vvoType: vvo.vvoType,
          testMeanName: vvo.testMean,
          systemName: vvo.system,
          applicability: vvo.applicability,
        });
        successCount++;
        addLog(`Row ${vvo.row}: Created "${vvo.summary}"`, 'success');
      } catch (err: any) {
        failCount++;
        addLog(`Row ${vvo.row}: Failed - ${err?.response?.data?.message || 'Unknown error'}`, 'error');
      }
    }

    addLog(`Campaign complete: ${successCount} created, ${failCount} failed`, successCount > 0 ? 'success' : 'error');
    setCreating(false);
    setCompleted(true);
  }

  function handleReset() {
    setFileName('');
    setCsvContent('');
    setParsedVvos([]);
    setLogs([]);
    setCompleted(false);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  const validCount = parsedVvos.filter(v => v.valid).length;
  const invalidCount = parsedVvos.filter(v => !v.valid).length;

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">Campaign Creation</h1>
          <p className="ads-page-subtitle">Bulk create VVOs from a CSV file</p>
        </div>
      </div>

      {/* Project and file input */}
      <div className="ads-card" style={{ marginBottom: 20 }}>
        <div className="ads-fields">
          <div className="ads-field">
            <label className="ads-field-label">Project ID</label>
            <input
              className="ads-field-input"
              value={projectInput}
              onChange={e => setProjectInput(e.target.value)}
              placeholder="Enter project ID"
            />
          </div>
        </div>
      </div>

      {/* Upload zone */}
      <div
        className="ads-upload-zone"
        style={{ marginBottom: 20 }}
        onClick={() => fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv"
          onChange={handleFileSelect}
          style={{ display: 'none' }}
        />
        {fileName ? (
          <div>
            <p style={{ fontWeight: 600, color: '#172b4d', marginBottom: 4 }}>{fileName}</p>
            <p style={{ fontSize: 12 }}>{parsedVvos.length} rows parsed</p>
          </div>
        ) : (
          <div>
            <p style={{ fontWeight: 600, color: '#172b4d', marginBottom: 4 }}>Click to upload CSV</p>
            <p style={{ fontSize: 12 }}>Expected columns: summary (required), description, vvoType, testMean, system, applicability</p>
          </div>
        )}
      </div>

      {/* Preview table */}
      {parsedVvos.length > 0 && (
        <div className="ads-section">
          <h3 className="ads-section-title">
            Preview ({validCount} valid, {invalidCount} invalid)
          </h3>
          <div className="ads-table-wrap" style={{ maxHeight: 400, overflowY: 'auto' }}>
            <table className="ads-table">
              <thead>
                <tr>
                  <th>Row</th>
                  <th>Summary</th>
                  <th>Type</th>
                  <th>Test Mean</th>
                  <th>System</th>
                  <th>Applicability</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {parsedVvos.map(v => (
                  <tr key={v.row} style={{ opacity: v.valid ? 1 : 0.5 }}>
                    <td>{v.row}</td>
                    <td>{v.summary || <span style={{ color: '#de350b' }}>MISSING</span>}</td>
                    <td>{v.vvoType || '-'}</td>
                    <td>{v.testMean || '-'}</td>
                    <td>{v.system || '-'}</td>
                    <td>{v.applicability || '-'}</td>
                    <td>
                      {v.valid ? (
                        <span className="ads-badge ads-badge--verified">Valid</span>
                      ) : (
                        <span className="ads-badge ads-badge--cancelled">{v.error}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
            <button
              className="ads-btn ads-btn--primary"
              onClick={handleCreateCampaign}
              disabled={creating || validCount === 0 || completed}
            >
              {creating ? 'Creating...' : `Create Campaign (${validCount} VVOs)`}
            </button>
            <button className="ads-btn" onClick={handleReset}>
              Reset
            </button>
          </div>
        </div>
      )}

      {/* Campaign log */}
      {logs.length > 0 && (
        <div className="ads-section" style={{ marginTop: 20 }}>
          <h3 className="ads-section-title">Campaign Log</h3>
          <div className="ads-card" style={{ maxHeight: 300, overflowY: 'auto', padding: 0 }}>
            {logs.map((log, i) => (
              <div
                key={i}
                style={{
                  padding: '6px 14px',
                  borderBottom: '1px solid #f4f5f7',
                  fontSize: 12,
                  fontFamily: 'monospace',
                  color: log.type === 'error' ? '#de350b' : log.type === 'success' ? '#006644' : '#172b4d',
                  background: log.type === 'error' ? '#ffebe6' : log.type === 'success' ? '#e3fcef' : 'transparent',
                }}
              >
                <span style={{ color: '#6b778c', marginRight: 8 }}>[{log.timestamp}]</span>
                {log.message}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
