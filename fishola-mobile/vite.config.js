import { defineConfig } from "vite";
import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue2';
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      img: path.resolve(__dirname, 'public/img'),
    }
  }
});
