/// <reference types="vitest" />
import { defineConfig } from "vite";
import solid from "vite-plugin-solid";
import devtools from "solid-devtools/vite";
import suidPlugin from "@suid/vite-plugin";

export default defineConfig({
  plugins: [suidPlugin(), solid(), devtools()],
  build: {
    target: "esnext",
  },
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
    }
  },
  test: {
    environment: "jsdom",
    testTransformMode: { web: ["/.tsx?$/"] },
    deps: {
      optimizer: {
        web: {
          enabled: false,
        },
      },
    },
  },
});
