import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    globals: true,           // 全局使用 describe, it, expect，无需每次导入
    environment: 'happy-dom', // 模拟浏览器 DOM 环境
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html']
    }
  }
})
