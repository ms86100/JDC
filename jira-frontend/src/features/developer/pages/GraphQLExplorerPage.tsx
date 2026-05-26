const base = import.meta.env.VITE_API_GATEWAY_URL ?? '';
const ISSUE_SERVICE_URL = base ? `${base}/graphql` : 'http://localhost:8084';
const GRAPHIQL_URL = base ? `${base}/graphiql` : 'http://localhost:8084/graphiql';

const SAMPLE_QUERY = `query {
  tests(projectId: "YOUR_PROJECT_UUID") {
    id
    name
    status
  }
}`;

export default function GraphQLExplorerPage() {
  return (
    <div className="flex flex-col h-[calc(100vh-120px)] p-4">
      <div className="mb-3">
        <h1 className="text-2xl font-bold">GraphQL API Explorer</h1>
        <p className="text-sm text-gray-500 mt-1">
          Interactive schema explorer for test management. Endpoint:{' '}
          <code className="bg-gray-100 px-1 rounded text-xs">POST /graphql</code>
          {' · '}
          <a
            href={GRAPHIQL_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-600 hover:underline"
          >
            Open in new tab
          </a>
        </p>
        <pre className="mt-2 text-xs bg-gray-50 border rounded p-2 overflow-x-auto">{SAMPLE_QUERY}</pre>
      </div>
      <iframe
        title="GraphiQL"
        src={GRAPHIQL_URL}
        className="flex-1 w-full border rounded-lg bg-white min-h-[500px]"
      />
    </div>
  );
}
