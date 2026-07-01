import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const GATEWAY = env.VITE_API_GATEWAY_URL || 'http://localhost:8080'
  // Proxy API calls (XHR/fetch) to the gateway, but serve index.html for full-page navigations.
  // Detect API calls by content-type or accept headers, not just Authorization (login has no token yet).
  const apiProxy = (target: string) => ({
    target,
    changeOrigin: true,
    bypass: (req: { headers?: Record<string, string | undefined> }) => {
      const h = req.headers || {};
      const isApiCall = h.authorization || h['content-type']?.includes('application/json') || h.accept?.includes('application/json');
      return isApiCall ? undefined : '/index.html';
    },
  })
  return {
    plugins: [react()],
    build: {
      sourcemap: false,
    },
    esbuild: {
      logOverride: { 'this-is-undefined-in-esm': 'silent' },
    },
    resolve: {
      alias: { '@': path.resolve(__dirname, './src') },
    },
    server: {
      host: true,
      port: 3000,
      strictPort: true,
      hmr: {
        overlay: process.env.VITE_HMR_OVERLAY !== 'false',
      },
      proxy: {
        // Auth - both /auth and /api/auth for compatibility
        '/auth': apiProxy(GATEWAY),
        '/api/auth': apiProxy(GATEWAY),
        // Projects
        '/projects': apiProxy(GATEWAY),
        '/api/projects': apiProxy(GATEWAY),
        // Templates
        '/templates': apiProxy(GATEWAY),
        '/api/templates': apiProxy(GATEWAY),
        // Issues
        '/issues': apiProxy(GATEWAY),
        '/api/issues': apiProxy(GATEWAY),
        // Workflows - direct to workflow-service to bypass gateway rate limiter
        '/workflows': apiProxy(env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085'),
        '/api/workflows': apiProxy(env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085'),
        // Workflow Schemes
        '/workflow-schemes': apiProxy(env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085'),
        '/api/workflow-schemes': apiProxy(env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085'),
        '/api/admin/workflows': apiProxy(env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085'),
        '/api/admin': apiProxy(GATEWAY),
        // Users
        '/user-service': apiProxy(GATEWAY),
        '/users': apiProxy(GATEWAY),
        '/api/users': apiProxy(GATEWAY),
        // Comments
        '/comments': apiProxy(GATEWAY),
        '/api/comments': apiProxy(GATEWAY),
        // Admin
        '/admin': apiProxy(GATEWAY),
        // Sprints - direct to sprint-service (8091) to bypass gateway auth
        '/sprints': apiProxy(env.VITE_SPRINT_SERVICE_URL || 'http://localhost:8091'),
        '/api/sprints': apiProxy(env.VITE_SPRINT_SERVICE_URL || 'http://localhost:8091'),
        // Boards - direct to sprint-service (8091)
        '/boards': apiProxy(env.VITE_SPRINT_SERVICE_URL || 'http://localhost:8091'),
        '/api/boards': apiProxy(env.VITE_SPRINT_SERVICE_URL || 'http://localhost:8091'),
        // Search
        '/search': apiProxy(GATEWAY),
        '/api/search': apiProxy(GATEWAY),
        // Notifications
        '/notifications': apiProxy(GATEWAY),
        '/api/notifications': apiProxy(GATEWAY),
        // Audit
        '/audit': apiProxy(GATEWAY),
        '/api/audit': apiProxy(GATEWAY),
        // Attachments
        '/attachments': apiProxy(GATEWAY),
        '/api/attachments': apiProxy(GATEWAY),
        // Versions
        '/versions': apiProxy(GATEWAY),
        '/api/versions': apiProxy(GATEWAY),
        // Components
        '/components': apiProxy(GATEWAY),
        '/api/components': apiProxy(GATEWAY),
        // Migration
        '/migration': apiProxy(GATEWAY),
        '/api/migration': apiProxy(GATEWAY),
        // Plans - route through gateway for /plans → /api/plans rewrite
        '/plans': apiProxy(GATEWAY),
        '/api/plans': apiProxy(GATEWAY),
        // Fields (dashboard gadgets)
        '/fields': apiProxy(GATEWAY),
        '/api/fields': apiProxy(GATEWAY),
        // Tests
        '/tests': apiProxy(GATEWAY),
        '/api/tests': apiProxy(GATEWAY),
        // GraphQL
        '/graphql': apiProxy(env.VITE_ISSUE_SERVICE_URL || 'http://localhost:8084'),
        '/graphiql': apiProxy(env.VITE_ISSUE_SERVICE_URL || 'http://localhost:8084'),
      },
    },
  }
})