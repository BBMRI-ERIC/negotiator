import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import git from 'git-rev-sync'


const PROXY_TARGET = "http://localhost:8081"

try {
  // eslint-disable-next-line
  process.env.VITE_GIT_TAG = git.tag()
} catch {
  try {
    // eslint-disable-next-line
    process.env.VITE_GIT_TAG = git.short('../.')
  } catch {
    // eslint-disable-next-line
    process.env.VITE_GIT_TAG = 'unknown'
  }
}

export default defineConfig(({ mode }) => {
  // eslint-disable-next-line
  const env = loadEnv(mode, process.cwd(), '')
  console.log(env)
  return {  
    plugins: [
      vue()
    ],
    build: {
      rollupOptions: {
        input: {
          index: fileURLToPath(new URL('./index.html', import.meta.url)),
          silentRenew: fileURLToPath(new URL('./silent-renew-oidc.html', import.meta.url)),
        },
      },
    },
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url))
      }
    },
    server: {
      port: 8080,
      cors: true,
      strictPort: false,
      proxy: {
        "/api": {
          target: PROXY_TARGET,
          changeOrigin: true,
          secure: false,
          ws: true,
          configure: (proxy) => {
            proxy.on('error', (err) => {
              console.log('proxy error', err);
            });
            proxy.on('proxyReq', (_proxyReq, req) => {
              console.log('Sending Request:', req.method, req.url);
            });
            proxy.on('proxyRes', (proxyRes, req) => {
              console.log('Received Response:', proxyRes.statusCode, req.url);
            });
          }
        },
        "/perun": {
          target: env.VITE_PERUN_API_URL,
          rewrite: (path) => path.replace(/^\/perun/, ''),
          changeOrigin: true,
          secure: true,
          ws: true,
          configure: (proxy) => {
            proxy.on('error', (err) => {
              console.log('proxy error', err);
            });
            proxy.on('proxyReq', (_proxyReq, req) => {
              console.log('Sending Request:', req.method, req.url);
            });
            proxy.on('proxyRes', (proxyRes, req) => {
              // Avoid CORS errors in development
              delete proxyRes.headers['access-control-allow-origin'];
              delete proxyRes.headers['access-control-allow-methods'];
              delete proxyRes.headers['access-control-allow-headers'];
              delete proxyRes.headers['access-control-allow-credentials'];
              console.log('Received Response:', proxyRes.statusCode, req.url);
            });
          }
        }
      }
    }
  }
})
