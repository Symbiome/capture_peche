/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
interface ViteTypeOptions {
  // By adding this line, you can make the type of ImportMetaEnv strict
  // to disallow unknown keys.
  // strictImportMetaEnv: unknown
}

interface ImportMetaEnv {
  readonly VITE__FORCED_DEVICE_TYPE: any;
  readonly VITE__REMOVE_PDF_VIEWER: string;
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
