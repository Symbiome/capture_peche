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
import moment from "moment";
import Helpers from "@/services/Helpers";

// Fonctions pures de formatage de dates et de durées : ce sont elles qui
// produisent tous les libellés de durée de sortie et d'heure de prise.
describe("Helpers — durées", () => {
  it("formate une durée en jours / heures / minutes", () => {
    const duration = moment.duration({ hours: 1, minutes: 30 });
    expect(Helpers.formatDuration(duration)).toBe("1h 30min ");
  });

  it("ajoute les secondes seulement si on le demande", () => {
    const duration = moment.duration({ minutes: 2, seconds: 5 });
    expect(Helpers.formatDuration(duration)).toBe("2min ");
    expect(Helpers.formatDuration(duration, true)).toBe("2min 5s ");
  });

  it("retombe sur un libellé nul plutôt que sur une chaîne vide", () => {
    const zero = moment.duration(0);
    expect(Helpers.formatDuration(zero)).toBe("0min");
    expect(Helpers.formatDuration(zero, true)).toBe("0s");
  });

  it("calcule une durée en secondes entre deux heures", () => {
    expect(Helpers.computeDurationInSeconds("10:00:00", "10:01:30")).toBe(90);
  });

  it("tronque à la minute inférieure au-delà d'une minute", () => {
    expect(Helpers.formatSecondsDurationTruncate(45)).toBe("45s");
    expect(Helpers.formatSecondsDurationTruncate(3725)).toBe("1h 2min ");
  });

  it("tronque une heure à la minute", () => {
    expect(Helpers.truncateTimeToMinutes("10:15:42")).toBe("10:15");
  });

  it("laisse passer une heure vide sans planter", () => {
    expect(Helpers.truncateTimeToMinutes("")).toBe("");
  });
});

describe("Helpers — dates", () => {
  it("formate une date au format ISO court", () => {
    expect(Helpers.formatToDate(new Date(2026, 0, 5))).toBe("2026-01-05");
    expect(Helpers.formatToDate(new Date(2026, 10, 25))).toBe("2026-11-25");
  });

  it("formate une heure avec un zéro de tête", () => {
    expect(Helpers.formatToHour(new Date(2026, 0, 5, 9, 5))).toBe("09h05");
    expect(Helpers.formatToHour(new Date(2026, 0, 5, 18, 42))).toBe("18h42");
  });

  it("relit un LocalDate du backend (mois en base 1)", () => {
    const date = Helpers.parseLocalDate([2026, 1, 5]);
    expect(Helpers.formatToDate(date)).toBe("2026-01-05");
  });

  it("relit un LocalDateTime du backend avec ou sans secondes", () => {
    expect(Helpers.formatToHour(Helpers.parseLocalDateTime([2026, 1, 5, 7, 30]))).toBe("07h30");
    expect(Helpers.formatToHour(Helpers.parseLocalDateTime([2026, 1, 5, 7, 30, 42]))).toBe("07h30");
  });

  it("compose une date et une heure saisies séparément", () => {
    const result = Helpers.parseDateTime(new Date(2026, 0, 5), "07:30");
    expect(Helpers.formatToDate(result)).toBe("2026-01-05");
    expect(Helpers.formatToHour(result)).toBe("07h30");
  });

  it("produit la valeur attendue par un <input type=date>", () => {
    expect(Helpers.toDateInputString([2026, 1, 5])).toBe("2026-01-05");
    expect(Helpers.toDateInputString([2026, 11, 25])).toBe("2026-11-25");
  });

  it("formate les dates longues en français", () => {
    expect(Helpers.formatToDateWithoutYear(new Date(2026, 0, 5))).toBe("5 janvier");
    expect(Helpers.formatToLongDate(new Date(2026, 0, 5))).toBe("lundi 5 janvier 2026");
  });
});

// Masque de saisie HH:mm utilisé par FormInput pour les champs "time" :
// le rendu natif du navigateur (24h ou AM/PM) dépend de la locale de
// l'appareil et non de la langue de l'app, on ne peut donc pas s'y fier.
describe("Helpers — masque de saisie horaire 24h", () => {
  it("insère le séparateur après les deux premiers chiffres", () => {
    expect(Helpers.maskTimeInput("1")).toBe("1");
    expect(Helpers.maskTimeInput("13")).toBe("13");
    expect(Helpers.maskTimeInput("130")).toBe("13:0");
    expect(Helpers.maskTimeInput("1300")).toBe("13:00");
  });

  it("ignore tout caractère non numérique (AM/PM, lettres, séparateurs déjà présents)", () => {
    expect(Helpers.maskTimeInput("13:00")).toBe("13:00");
    expect(Helpers.maskTimeInput("1a3b0c0")).toBe("13:00");
    expect(Helpers.maskTimeInput("01:00 PM")).toBe("01:00");
  });

  it("tronque au-delà de 4 chiffres (HH:mm)", () => {
    expect(Helpers.maskTimeInput("235959")).toBe("23:59");
  });

  it("valide uniquement les heures au format 24h complet", () => {
    expect(Helpers.isValidTimeString("00:00")).toBe(true);
    expect(Helpers.isValidTimeString("23:59")).toBe(true);
    expect(Helpers.isValidTimeString("13:45")).toBe(true);
    expect(Helpers.isValidTimeString("24:00")).toBe(false);
    expect(Helpers.isValidTimeString("13:60")).toBe(false);
    expect(Helpers.isValidTimeString("13:0")).toBe(false);
    expect(Helpers.isValidTimeString("1:00 PM")).toBe(false);
    expect(Helpers.isValidTimeString("")).toBe(false);
  });
});

// Alerte de déclaration obligatoire (#91), déclenchée après l'enregistrement
// d'une capture d'une espèce soumise à déclaration.
describe("Helpers — alerte de déclaration obligatoire", () => {
  function fakeModal() {
    let shown: any = null;
    return {
      show: (name: string, params: any) => {
        shown = params;
      },
      hide: () => {
        shown = null;
      },
      get shown() {
        return shown;
      },
    };
  }

  it("propose d'ouvrir le lien officiel quand il est renseigné", async () => {
    const modal = fakeModal();
    const openSpy = vi.spyOn(window, "open").mockImplementation(() => null);

    const promise = Helpers.declareCatchAlert(modal, "https://federation.example/declarer");
    expect(modal.shown.buttons).toHaveLength(2);

    const declareButton = modal.shown.buttons.find((b: any) => b.title === "Déclarer");
    declareButton.handler();
    await promise;

    expect(openSpy).toHaveBeenCalledWith("https://federation.example/declarer", "_blank");
    openSpy.mockRestore();
  });

  it("affiche un message générique et un seul bouton sans lien officiel", async () => {
    const modal = fakeModal();
    const promise = Helpers.declareCatchAlert(modal, undefined);

    expect(modal.shown.buttons).toHaveLength(1);
    expect(modal.shown.text).toContain("Contactez votre fédération");

    modal.shown.buttons[0].handler();
    await promise;
  });
});
