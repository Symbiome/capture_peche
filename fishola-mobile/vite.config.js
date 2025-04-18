import { defineConfig, loadEnv } from "vite";
import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue2';
import path from "path";
import fs from "fs";
import packageJson from "./package.json";
import { visualizer } from "rollup-plugin-visualizer";

const excludeLibrariesInDevModePlugin = (mode) => {
  const env = loadEnv(mode, process.cwd());
  return {
    name: "exclude-lib-files",
    apply: "build",
    buildStart() {
      const removePDFView = env.VITE__REMOVE_PDF_VIEWER === "true";
      const removeOpenCV = env.VITE__REMOVE_OPENCV === "true";
      console.warn("Build for environment " + mode + " : ", env);
      const filePath = path.resolve(__dirname, "public/js/opencv.js");
      if (removeOpenCV) {
        if (mode === "mobile" || mode === "web") {
          throw new Error(
            "This is a production build, you must set VITE__REMOVE_OPENCV to false"
          );
        }
        if (fs.existsSync(filePath)) {
          fs.unlinkSync(filePath);
          console.warn("🔧 public/opencv.js will be removed from build");
        } else {
          console.warn("🔧 public/opencv.js will be removed from build (already ignored)");
        }
      } else {
        if (fs.existsSync(filePath)) {
          console.warn("🔧 public/opencv.js is included in build");
        } else {
          throw new Error(
            "Missing public/js/opencv.js file"
          );
        }
      }
      console.warn(
        "🔧 PDFViewer library is " +
          (removePDFView ? "removed from" : "included in") +
          " build"
      );
    },
  };
};



// https://vitejs.dev/config/
export default defineConfig( ({mode}) => {
  const { gitDescribeSync } = require("git-describe");
  const gitRevision = gitDescribeSync().suffix;
  const frontendVersion = packageJson.version;
  const mvnVersion = process.env.MAVEN_PROJECT_VERSION || "N/A";

  return {
    plugins: [
      vue(),
      excludeLibrariesInDevModePlugin(mode),
      visualizer({
        filename: "./stats.html",
        gzipSize: true,
        brotliSize: true,
      }),
    ],
    server: {
      port: 8081,
      hmr: {
        overlay: false,
      },
    },
    build: {
      outDir: "target/dist-" + mode,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes("node_modules/vue-pdf-app")) {
              return "pdf-lib";
            }
            if (id.includes("leaflet")) {
              return "leaflet";
            }
            if (id.includes("canvas")) {
              return "canvas";
            }
            if (id.includes("node_modules")) {
              return "vendor";
            }
          },
        },
      },
    },
    define: {
      "import.meta.env.VITE__PACKAGE_JSON_VERSION": JSON.stringify(frontendVersion),
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
  };
});
