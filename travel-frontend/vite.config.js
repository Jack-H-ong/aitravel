/**
 * Vite 配置（f-foundation）。
 *
 * <p>dev server 代理 `/api` → 后端 `http://localhost:8080`
 * （`changeOrigin: true`，**路径不重写**，因为后端接口本身即以 `/api` 为前缀）。
 */
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
