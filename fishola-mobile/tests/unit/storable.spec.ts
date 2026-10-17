import { reactive } from "vue";
import { toStorable } from "@/services/Storable";

/** Écriture IndexedDB d'objets réactifs Vue 3 (#284). */
describe("toStorable", () => {
  it("retire les Proxy réactifs, y compris imbriqués, pour le clonage structuré", () => {
    const trip = reactive({ name: "Sortie", catchs: [{ size: 42 }], meta: { lakeId: "l1" } });
    const tripBean = { ...trip };

    const storable = toStorable(tripBean);

    expect(() => structuredClone(tripBean)).toThrow();
    expect(structuredClone(storable)).toEqual({ name: "Sortie", catchs: [{ size: 42 }], meta: { lakeId: "l1" } });
  });

  it("conserve les dates et les binaires tels quels", () => {
    const date = new Date(2026, 9, 10);
    const blob = new Blob(["photo"]);

    const storable = toStorable(reactive({ date, blob }));

    expect(storable.date).toBe(date);
    expect(storable.blob).toBe(blob);
  });
});
