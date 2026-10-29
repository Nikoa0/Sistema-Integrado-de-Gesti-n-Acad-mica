import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In development, /api is proxied to the Spring Boot backend (no CORS needed).
// In production, Nginx does the same job.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
