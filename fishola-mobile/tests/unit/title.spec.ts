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
import { shallowMount } from "@vue/test-utils";
import Title from "@/components/layout/Title.vue";

// Ce test sert aussi de garde-fou sur la configuration : monter ce composant
// exerce d'un coup le plugin Vue 2, l'alias « @ », TypeScript et ses
// décorateurs, le bloc <style lang="less"> et ses variables, et la
// substitution des import.meta.env. S'il tombe, c'est l'outillage qui a cassé.
describe("Title", () => {
  it("affiche le logo de l'application", () => {
    const wrapper = shallowMount(Title);
    expect(wrapper.find("img").attributes("alt")).toBe("FISHOLA");
  });

  it("affiche le nom d'environnement issu de la configuration Vite", () => {
    const wrapper = shallowMount(Title);
    const envName = import.meta.env.VITE__ENV_NAME;
    if (envName) {
      expect(wrapper.find("span.env").text()).toBe("(" + envName + ")");
    } else {
      expect(wrapper.find("span.env").exists()).toBe(false);
    }
  });

  it("renvoie vers l'accueil au clic", async () => {
    const push = vi.fn().mockResolvedValue(undefined);
    const wrapper = shallowMount(Title, { mocks: { $router: { push } } });
    await wrapper.find(".header-title").trigger("click");
    expect(push).toHaveBeenCalledWith("/my-trips/list");
  });
});
