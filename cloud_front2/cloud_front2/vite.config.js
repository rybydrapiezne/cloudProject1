import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  // Load env variables based on mode (development/production)
  const env = loadEnv(mode, process.cwd(), 'VITE_')

  return {
    plugins: [react()],
    preview: {
      port: 5173,
      strictPort: true,
      host: '0.0.0.0',
      allowedHosts: true,
    },
  }
})