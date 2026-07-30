import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { issueTestOpsApi } from '../../../api/issueTestOpsApi';

export default function AiTestPage() {
  const [requirement, setRequirement] = useState('');
  const [projectId, setProjectId] = useState('');
  const [requirementKeys, setRequirementKeys] = useState('');
  const [result, setResult] = useState<unknown>(null);

  const suggestMutation = useMutation({
    mutationFn: () => issueTestOpsApi.suggestTests(requirement),
    onSuccess: (res) => setResult(res.data),
  });

  const coverageMutation = useMutation({
    mutationFn: () =>
      issueTestOpsApi.getCoverageRecommendations(
        projectId,
        requirementKeys.split(',').map((k) => k.trim()).filter(Boolean),
      ),
    onSuccess: (res) => setResult(res.data),
  });

  const duplicatesMutation = useMutation({
    mutationFn: () => issueTestOpsApi.analyzeDuplicates([]),
    onSuccess: (res) => setResult(res.data),
  });

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">AI Test Assistant</h1>
      <p className="text-sm text-gray-500 mb-6">
        Coverage recommendations, test suggestions, duplicate analysis, and risk assessment.
      </p>

      <section className="border rounded-lg p-4 mb-4 bg-white">
        <h2 className="font-semibold mb-2">Suggest tests from requirement</h2>
        <textarea
          className="w-full border rounded px-3 py-2 text-sm mb-2"
          rows={3}
          placeholder="Describe the requirement…"
          value={requirement}
          onChange={(e) => setRequirement(e.target.value)}
        />
        <button
          type="button"
          className="px-4 py-2 bg-blue-600 text-white rounded text-sm disabled:opacity-50"
          disabled={!requirement.trim() || suggestMutation.isPending}
          onClick={() => suggestMutation.mutate()}
        >
          Suggest tests
        </button>
      </section>

      <section className="border rounded-lg p-4 mb-4 bg-white">
        <h2 className="font-semibold mb-2">Coverage recommendations</h2>
        <input
          className="w-full border rounded px-3 py-2 text-sm mb-2"
          placeholder="Project UUID"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
        />
        <input
          className="w-full border rounded px-3 py-2 text-sm mb-2"
          placeholder="Requirement keys (comma-separated)"
          value={requirementKeys}
          onChange={(e) => setRequirementKeys(e.target.value)}
        />
        <button
          type="button"
          className="px-4 py-2 bg-blue-600 text-white rounded text-sm disabled:opacity-50"
          disabled={!projectId.trim() || coverageMutation.isPending}
          onClick={() => coverageMutation.mutate()}
        >
          Get recommendations
        </button>
      </section>

      <section className="border rounded-lg p-4 mb-4 bg-white">
        <h2 className="font-semibold mb-2">Analyze duplicates</h2>
        <button
          type="button"
          className="px-4 py-2 border rounded text-sm"
          onClick={() => duplicatesMutation.mutate()}
          disabled={duplicatesMutation.isPending}
        >
          Run duplicate analysis
        </button>
      </section>

      {result != null && (
        <pre className="border rounded-lg p-4 bg-gray-50 text-xs overflow-auto max-h-96">
          {JSON.stringify(result, null, 2)}
        </pre>
      )}
    </div>
  );
}
