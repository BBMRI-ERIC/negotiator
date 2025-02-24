import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
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

export default defineConfig({
    plugins: [
        vue()
    ],
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url))
        }
    },
    server: {
        port: 8080,
        proxy: {
            "^/api": {
                target: PROXY_TARGET,
                changeOrigin: true
            }
        }
    }
})
