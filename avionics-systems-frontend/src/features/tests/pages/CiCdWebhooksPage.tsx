import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { issueTestOpsApi } from '../../../api/issueTestOpsApi';

type Provider = 'github' | 'jenkins' | 'gitlab' | 'azure';

export default function CiCdWebhooksPage() {
  const [projectId, setProjectId] = useState('');
  const [provider, setProvider] = useState<Provider>('github');
  const [payloadJson, setPayloadJson] = useState('{\n  "buildStatus": "SUCCESS"\n}');
  const [result, setResult] = useState<unknown>(null);

  const sendMutation = useMutation({
    mutationFn: async () => {
      const payload = JSON.parse(payloadJson) as Record<string, unknown>;
      switch (provider) {
        case 'jenkins':
          return issueTestOpsApi.sendJenkinsWebhook(projectId, payload);
        case 'gitlab':
          return issueTestOpsApi.sendGitLabWebhook(projectId, payload);
        case 'azure':
          return issueTestOpsApi.sendAzureDevOpsWebhook(projectId, payload);
        default:
          return issueTestOpsApi.sendGitHubWebhook(projectId, payload);
      }
    },
    onSuccess: (res) => setResult(res.data),
    onError: (err: Error) => setResult({ error: err.message }),
  });

  const triggerMutation = useMutation({
    mutationFn: async () => {
      const payload = JSON.parse(payloadJson) as Record<string, unknown>;
      return issueTestOpsApi.triggerCiExecution(projectId, payload);
    },
    onSuccess: (res) => setResult(res.data),
    onError: (err: Error) => setResult({ error: err.message }),
  });

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">CI/CD Webhooks</h1>
      <p className="text-sm text-gray-500 mb-6">
        Simulate GitHub Actions, Jenkins, GitLab CI, or Azure DevOps webhooks and trigger test runs.
      </p>

      <div className="border rounded-lg p-4 bg-white space-y-3">
        <input
          className="w-full border rounded px-3 py-2 text-sm"
          placeholder="Project UUID *"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
        />
        <select
          className="w-full border rounded px-3 py-2 text-sm"
          value={provider}
          onChange={(e) => setProvider(e.target.value as Provider)}
        >
          <option value="github">GitHub Actions</option>
          <option value="jenkins">Jenkins</option>
          <option value="gitlab">GitLab CI</option>
          <option value="azure">Azure DevOps</option>
        </select>
        <textarea
          className="w-full border rounded px-3 py-2 text-sm font-mono"
          rows={8}
          value={payloadJson}
          onChange={(e) => setPayloadJson(e.target.value)}
        />
        <div className="flex gap-2">
          <button
            type="button"
            className="px-4 py-2 bg-blue-600 text-white rounded text-sm disabled:opacity-50"
            disabled={!projectId.trim() || sendMutation.isPending}
            onClick={() => sendMutation.mutate()}
          >
            Send webhook
          </button>
          <button
            type="button"
            className="px-4 py-2 border rounded text-sm disabled:opacity-50"
            disabled={!projectId.trim() || triggerMutation.isPending}
            onClick={() => triggerMutation.mutate()}
          >
            Trigger execution
          </button>
        </div>
      </div>

      {result != null && (
        <pre className="mt-4 border rounded-lg p-4 bg-gray-50 text-xs overflow-auto max-h-64">
          {JSON.stringify(result, null, 2)}
        </pre>
      )}
    </div>
  );
}
