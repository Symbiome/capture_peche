interface ViteTypeOptions {
  // By adding this line, you can make the type of ImportMetaEnv strict
  // to disallow unknown keys.
  // strictImportMetaEnv: unknown
}

interface ImportMetaEnv {
  VITE__REMOVE_PDF_VIEWER: string;
  readonly VITE__MVN_VERSION: string;
  readonly VITE__PACKAGE_JSON_VERSION: string;
  readonly VITE__GIT_REVISION: string;
  readonly VITE__ENV_NAME: string;
  readonly VITE__API_URL: string;
  readonly VITE__API_DEFAULT_PORT: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}