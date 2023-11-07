import { defineConfig } from "vite";
import solid from "vite-plugin-solid";
import devtools from "solid-devtools/vite";
import suidPlugin from "@suid/vite-plugin";
import { viteSingleFile } from "vite-plugin-singlefile";

export default defineConfig({
  plugins: [suidPlugin(), solid(), devtools(), viteSingleFile()],
  build: {
    target: "esnext",
  },
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
