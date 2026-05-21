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
        '/api/migration': {
          target: env.VITE_MIGRATION_SERVICE_URL || 'http://localhost:8094',
          changeOrigin: true,
        },
        '/api': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
