/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
import { defineConfig, mergeConfig } from "vite";
import viteConfig from "./vite.config.js";

// La configuration de test réutilise telle quelle celle de vite.config.js
// (alias « @ », plugin Vue 2, préprocesseur Less, define des import.meta.env)
// afin de ne pas la dupliquer : les tests s'exécutent avec la même résolution
// de modules que l'application.
export default defineConfig((env) =>
  mergeConfig(viteConfig(env), {
    test: {
      globals: true,
      environment: "jsdom",
      setupFiles: ["tests/unit/vitest.setup.ts"],
      include: ["tests/unit/**/*.spec.ts"],
      // Une suite vide doit rester rouge : sans test exécuté, le récapitulatif
      // de run_tests.sh mentirait en annonçant « tout est vert ».
      passWithNoTests: false,
    },
  })
);
