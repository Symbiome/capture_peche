/*-
 * #%L
 * Fishola :: Mobile
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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
import ScaleSampling from "@/services/ScaleSampling";
import ProfileService from "@/services/ProfileService";
import DocumentationService from "@/services/DocumentationService";
import SettingsView from "@/views/Settings.vue";

describe("ScaleSampling.showSampleToggle", () => {
  it("masque le toggle quand la fonctionnalité est désactivée, même avec promptSamples", () => {
    expect(ScaleSampling.showSampleToggle(false, false, true, true)).toBe(false);
  });

  it("masque le toggle d'une capture existante avec numéro d'échantillon quand désactivée", () => {
    expect(ScaleSampling.showSampleToggle(false, true, true, true)).toBe(false);
  });

  it("conserve le comportement historique quand la fonctionnalité est activée", () => {
    expect(ScaleSampling.showSampleToggle(true, false, true, true)).toBe(true);
    expect(ScaleSampling.showSampleToggle(true, true, false, false)).toBe(true);
    expect(ScaleSampling.showSampleToggle(true, false, false, true)).toBe(false);
    expect(ScaleSampling.showSampleToggle(true, false, true, false)).toBe(false);
  });

  it("est désactivée par la configuration par défaut", () => {
    expect(ScaleSampling.isEnabled()).toBe(false);
  });
});

describe("Settings — prélèvement d'écailles", () => {
  const settings = { promptWeight: true, promptSamples: true };

  beforeEach(() => {
    vi.spyOn(ProfileService, "getSettings").mockResolvedValue({ ...settings } as any);
    vi.spyOn(DocumentationService, "getSamplesDocumentationUrl").mockReturnValue("");
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  async function mountSettings(enabled: boolean) {
    vi.spyOn(ScaleSampling, "isEnabled").mockReturnValue(enabled);
    const wrapper = shallowMount(SettingsView);
    await new Promise((resolve) => setTimeout(resolve));
    return wrapper;
  }

  it("masque le réglage et l'encart collecteur quand la fonctionnalité est désactivée", async () => {
    const wrapper = await mountSettings(false);
    expect(wrapper.text()).toContain("Renseigner le poids des captures");
    expect(wrapper.text()).not.toContain("Effectuer des prélèvements");
    expect(wrapper.find("bottominducementview-stub").exists()).toBe(false);
  });

  it("affiche le réglage et l'encart collecteur quand la fonctionnalité est activée", async () => {
    const wrapper = await mountSettings(true);
    expect(wrapper.text()).toContain("Effectuer des prélèvements");
    expect(wrapper.find("bottominducementview-stub").exists()).toBe(true);
  });
});
