import React, { useState, useRef } from 'react';
import combinedApi, { TestImportResponse } from '../../../api/testApi';

interface ImportPanelProps {
  projectId: string;
  onImportComplete?: () => void;
}

export const ImportPanel: React.FC<ImportPanelProps> = ({ projectId, onImportComplete }) => {
  const [importType, setImportType] = useState<'CUCUMBER' | 'JUNIT' | 'TESTNG' | 'NUNIT' | 'ROBOT'>('CUCUMBER');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<TestImportResponse | null>(null);
  const [error, setError] = useState('');
  const [history, setHistory] = useState<TestImportResponse[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadHistory = async () => {
    try {
      const response = await combinedApi.getImportHistory(projectId);
      setHistory(response);
    } catch (error) {
      console.error('Failed to load import history:', error);
    }
  };

  React.useEffect(() => {
    loadHistory();
  }, [projectId]);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError('');
      setImportResult(null);
    }
  };

  const handleImport = async () => {
    if (!selectedFile) {
      setError('Please select a file to import');
      return;
    }

    setImporting(true);
    setError('');

    try {
      const importFn = {
        CUCUMBER: combinedApi.importCucumber,
        JUNIT: combinedApi.importJUnit,
        TESTNG: combinedApi.importTestNg,
        NUNIT: combinedApi.importNUnit,
        ROBOT: combinedApi.importRobot,
      }[importType];
      const response = await importFn(projectId, selectedFile);
      setImportResult(response);
      onImportComplete?.();
      loadHistory();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Import failed');
    } finally {
      setImporting(false);
    }
  };

  const getFileIcon = (fileName: string) => {
    if (fileName.endsWith('.feature')) return '🥒';
    if (fileName.endsWith('.xml')) return '📄';
    return '📁';
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED': return '✅';
      case 'FAILED': return '❌';
      case 'PROCESSING': return '⏳';
      default: return '⏸️';
    }
  };

  return (
    <div className="import-panel">
      <h3 className="text-lg font-semibold mb-4">Import Tests</h3>

      {/* Import Type Selection */}
      <div className="import-type-selector mb-4">
        <div className="flex gap-4">
          <label className={`flex items-center p-3 border rounded cursor-pointer ${
            importType === 'CUCUMBER' ? 'border-blue-500 bg-blue-50' : ''
          }`}>
            <input
              type="radio"
              name="importType"
              value="CUCUMBER"
              checked={importType === 'CUCUMBER'}
              onChange={() => setImportType('CUCUMBER')}
              className="mr-2"
            />
            <span className="text-2xl mr-2">🥒</span>
            <div>
              <div className="font-medium">Cucumber / Gherkin</div>
              <div className="text-sm text-gray-500">Import .feature files</div>
            </div>
          </label>

          <label className={`flex items-center p-3 border rounded cursor-pointer ${
            importType === 'JUNIT' ? 'border-blue-500 bg-blue-50' : ''
          }`}>
            <input
              type="radio"
              name="importType"
              value="JUNIT"
              checked={importType === 'JUNIT'}
              onChange={() => setImportType('JUNIT')}
              className="mr-2"
            />
            <span className="text-2xl mr-2">📄</span>
            <div>
              <div className="font-medium">JUnit XML</div>
              <div className="text-sm text-gray-500">Import CI/CD test results</div>
            </div>
          </label>

          <label className={`flex items-center p-3 border rounded cursor-pointer ${
            importType === 'TESTNG' ? 'border-blue-500 bg-blue-50' : ''
          }`}>
            <input
              type="radio"
              name="importType"
              value="TESTNG"
              checked={importType === 'TESTNG'}
              onChange={() => setImportType('TESTNG')}
              className="mr-2"
            />
            <span className="text-2xl mr-2">🧪</span>
            <div>
              <div className="font-medium">TestNG XML</div>
              <div className="text-sm text-gray-500">Import TestNG results</div>
            </div>
          </label>

          <label className={`flex items-center p-3 border rounded cursor-pointer ${
            importType === 'NUNIT' ? 'border-blue-500 bg-blue-50' : ''
          }`}>
            <input
              type="radio"
              name="importType"
              value="NUNIT"
              checked={importType === 'NUNIT'}
              onChange={() => setImportType('NUNIT')}
              className="mr-2"
            />
            <span className="text-2xl mr-2">🔷</span>
            <div>
              <div className="font-medium">NUnit XML</div>
              <div className="text-sm text-gray-500">Import NUnit v2/v3 results</div>
            </div>
          </label>

          <label className={`flex items-center p-3 border rounded cursor-pointer ${
            importType === 'ROBOT' ? 'border-blue-500 bg-blue-50' : ''
          }`}>
            <input
              type="radio"
              name="importType"
              value="ROBOT"
              checked={importType === 'ROBOT'}
              onChange={() => setImportType('ROBOT')}
              className="mr-2"
            />
            <span className="text-2xl mr-2">🤖</span>
            <div>
              <div className="font-medium">Robot Framework</div>
              <div className="text-sm text-gray-500">Import output.xml</div>
            </div>
          </label>
        </div>
      </div>

      {/* File Selection */}
      <div className="file-selection mb-4">
        <div className="border-2 border-dashed rounded-lg p-6 text-center hover:border-blue-400 transition-colors">
          <input
            ref={fileInputRef}
            type="file"
            onChange={handleFileSelect}
            accept={importType === 'CUCUMBER' ? '.feature,.zip' : '.xml,.zip,.json'}
            className="hidden"
          />
          {selectedFile ? (
            <div className="flex items-center justify-center gap-3">
              <span className="text-3xl">{getFileIcon(selectedFile.name)}</span>
              <div className="text-left">
                <div className="font-medium">{selectedFile.name}</div>
                <div className="text-sm text-gray-500">
                  {(selectedFile.size / 1024).toFixed(1)} KB
                </div>
              </div>
              <button
                onClick={() => {
                  setSelectedFile(null);
                  if (fileInputRef.current) fileInputRef.current.value = '';
                }}
                className="ml-4 text-red-500 hover:text-red-700"
              >
                ✕
              </button>
            </div>
          ) : (
            <button
              onClick={() => fileInputRef.current?.click()}
              className="btn btn-secondary"
            >
              Choose File
            </button>
          )}
        </div>
        <p className="text-xs text-gray-500 mt-2">
          {importType === 'CUCUMBER'
            ? 'Accepts .feature files or .zip archives containing feature files'
            : `Accepts ${importType} XML files or .zip archives`}
        </p>
      </div>

      {/* Import Button */}
      <button
        onClick={handleImport}
        disabled={!selectedFile || importing}
        className="btn btn-primary w-full"
      >
        {importing ? (
          <span className="flex items-center justify-center gap-2">
            <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
            Importing...
          </span>
        ) : (
          `Import ${importType} File`
        )}
      </button>

      {/* Error Message */}
      {error && (
        <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded text-red-600 text-sm">
          {error}
        </div>
      )}

      {/* Import Result */}
      {importResult && (
        <div className={`mt-4 p-4 rounded-lg ${
          importResult.status === 'COMPLETED' ? 'bg-green-50' : 'bg-red-50'
        }`}>
          <div className="flex items-center gap-2 mb-3">
            <span className="text-xl">{getStatusIcon(importResult.status)}</span>
            <span className="font-semibold">
              {importResult.status === 'COMPLETED' ? 'Import Successful!' : 'Import Failed'}
            </span>
          </div>
          <div className="grid grid-cols-3 gap-3 text-sm">
            <div>
              <div className="text-gray-500">Tests Created</div>
              <div className="text-xl font-semibold">{importResult.totalTests}</div>
            </div>
            <div>
              <div className="text-gray-500">Passed</div>
              <div className="text-xl font-semibold text-green-600">{importResult.passedTests}</div>
            </div>
            <div>
              <div className="text-gray-500">Failed</div>
              <div className="text-xl font-semibold text-red-600">{importResult.failedTests}</div>
            </div>
          </div>
          {importResult.errorMessage && (
            <div className="mt-3 text-sm text-red-600">
              Error: {importResult.errorMessage}
            </div>
          )}
        </div>
      )}

      {/* Import History */}
      <div className="import-history mt-6">
        <h4 className="font-medium mb-3">Recent Imports</h4>
        {history.length === 0 ? (
          <p className="text-sm text-gray-500">No import history</p>
        ) : (
          <div className="space-y-2">
            {history.slice(0, 10).map((item) => (
              <div
                key={item.id}
                className="flex items-center justify-between p-2 bg-gray-50 rounded hover:bg-gray-100"
              >
                <div className="flex items-center gap-3">
                  <span>{getFileIcon(item.fileName)}</span>
                  <div>
                    <div className="text-sm font-medium">{item.fileName}</div>
                    <div className="text-xs text-gray-500">
                      {item.importType} • {item.totalTests} tests • {new Date(item.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>
                <span className={`px-2 py-0.5 rounded text-xs ${
                  item.status === 'COMPLETED' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                }`}>
                  {item.status}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ImportPanel;