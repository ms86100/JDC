import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
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
      // Bind IPv4 + IPv6 so http://127.0.0.1:3000 and http://localhost:3000 both work on Windows.
      host: true,
      port: 3000,
      strictPort: true,
      hmr: {
        overlay: process.env.VITE_HMR_OVERLAY !== 'false',
      },
      proxy: {
        // Workflow admin APIs — direct to workflow service when gateway is not running
        '/api/workflow-schemes': {
          target: env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true,
        },
        '/api/workflows': {
          target: env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true,
        },
        '/api/admin/workflows': {
          target: env.VITE_WORKFLOW_SERVICE_URL || 'http://localhost:8085',
          changeOrigin: true,
        },
        '/api/fields': {
          target: env.VITE_MIGRATION_SERVICE_URL || 'http://localhost:8094',
          changeOrigin: true,
        },
        '/api/custom-fields': {
          target: env.VITE_MIGRATION_SERVICE_URL || 'http://localhost:8094',
          changeOrigin: true,
        },
        '/api/migration': {
          target: env.VITE_MIGRATION_SERVICE_URL || 'http://localhost:8094',
          changeOrigin: true,
        },
        '/graphql': {
          target: env.VITE_ISSUE_SERVICE_URL || 'http://localhost:8084',
          changeOrigin: true,
        },
        '/graphiql': {
          target: env.VITE_ISSUE_SERVICE_URL || 'http://localhost:8084',
          changeOrigin: true,
        },
        '/api/comments': {
          target: env.VITE_COMMENT_SERVICE_URL || 'http://127.0.0.1:8086',
          changeOrigin: true,
        },
        '/api/boards': {
          target: env.VITE_SPRINT_SERVICE_URL || 'http://localhost:8091',
          changeOrigin: true,
        },
        '/api/sprints': {
          target: env.VITE_SPRINT_SERVICE_URL || 'http://localhost:8091',
          changeOrigin: true,
        },
        '/api': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
          timeout: 120_000,
          proxyTimeout: 120_000,
        },
      },
    },
  }
})
