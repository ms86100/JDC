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
        // All API routes proxy to gateway
        '/auth': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/projects': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/issues': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/workflows': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/users': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/comments': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/admin': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/sprints': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/boards': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/search': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/notifications': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/audit': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/attachments': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/versions': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/components': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/migration': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
        '/tests': {
          target: env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
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
      },
    },
  }
})