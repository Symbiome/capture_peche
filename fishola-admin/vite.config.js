import { defineConfig } from "vite";
import { fileURLToPath, URL } from "node:url";
import vue from "@vitejs/plugin-vue";
import path from "path";
import packageJson from "./package.json";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const { gitDescribeSync } = require("git-describe");
  const gitRevision = gitDescribeSync().suffix;
  const frontendVersion = packageJson.version;
  const mvnVersion = process.env.MAVEN_PROJECT_VERSION || "N/A";
  const base = mode == 'production' ? '/admin/': "/";

  return {
    base: base,
    plugins: [
      vue({})
    ],
    server: {
      port: 8082,
      hmr: {
        overlay: false,
      },
    },
    build: {
      outDir: "target/dist-" + mode,
    },
    define: {
      "import.meta.env.VITE__PACKAGE_JSON_VERSION":
        JSON.stringify(frontendVersion),
      "import.meta.env.VITE__GIT_REVISION": JSON.stringify(gitRevision),
      "import.meta.env.VITE__MVN_VERSION": JSON.stringify(mvnVersion),
    },
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
        img: path.resolve(__dirname, "public/img"),
        fonts: path.resolve(__dirname, "public/fonts"),
      },
    },
    css: {
      preprocessorOptions: {
        less: {
          javascriptEnabled: true,
          additionalData: `@import "@/less/main.less";`,
        },
      },
    },
  };
});
