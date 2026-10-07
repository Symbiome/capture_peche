export function showLink(event: Event, url: string) {
  // Do not foward click event to row (would trigger modal)
  event.stopPropagation();

  window.open(url, "_blank");
}

const TIME_24H_REGEX = /^([01]\d|2[0-3]):[0-5]\d$/;

// Masque de saisie HH:mm indépendant de la locale du navigateur/OS : les
// <input type="time"> natifs affichent AM/PM selon la locale de l'appareil,
// pas selon la langue de l'app.
export function maskTimeInput(raw: string): string {
  const digits = (raw || "").replace(/\D/g, "").slice(0, 4);
  if (digits.length <= 2) {
    return digits;
  }
  return `${digits.slice(0, 2)}:${digits.slice(2)}`;
}

export function isValidTimeString(value: string): boolean {
  return TIME_24H_REGEX.test(value);
}

function padTwoDigits(value: number): string {
  return String(value).padStart(2, "0");
}

/**
 * Date locale de l'appareil au format AAAA-MM-JJ (#234). Contrairement à
 * `toISOString()`, qui renvoie la date UTC, ne donne pas la veille entre minuit
 * et 2 h en France l'été.
 */
export function localDateIso(date: Date = new Date()): string {
  return `${date.getFullYear()}-${padTwoDigits(date.getMonth() + 1)}-${padTwoDigits(date.getDate())}`;
}

/** Heure locale de l'appareil au format HH:mm, minutes tronquées (#234). */
export function localTimeHHmm(date: Date = new Date()): string {
  return `${padTwoDigits(date.getHours())}:${padTwoDigits(date.getMinutes())}`;
}

/**
 * Fin d'une sortie postérieure à son début (#237) ; une date de fin vide vaut la date de
 * début (sortie d'une journée). Null tant que les heures ne sont pas des HH:mm valides.
 */
export function isEndAfterStart(day: string, startTime: string, endDay: string, endTime: string): boolean | null {
  if (!isValidTimeString(startTime) || !isValidTimeString(endTime)) {
    return null;
  }
  return `${endDay || day}T${endTime}` > `${day}T${startTime}`;
}

// Minuscules, sans accents, tirets et apostrophes remplacés par des espaces,
// article initial retiré : « Saône » et « saone », « Chalon-sur-Saône » et
// « chalon sur saone », « le Rhône » et « rhone » se valent (#197).
export function normalizeSearchText(value: string): string {
  return (value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[-'’]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/^(le|la|les|l) /, "");
}

// Classement d'un libellé pour une recherche (#197, #203) : égalité exacte, puis
// préfixe, puis début de mot, puis simple inclusion. null si le libellé ne correspond pas.
export function searchRank(label: string, query: string): number | null {
  const normalizedLabel = normalizeSearchText(label);
  const normalizedQuery = normalizeSearchText(query);
  if (!normalizedQuery) {
    return 3;
  }
  if (normalizedLabel === normalizedQuery) {
    return 0;
  }
  if (normalizedLabel.startsWith(normalizedQuery)) {
    return 1;
  }
  if (normalizedLabel.includes(" " + normalizedQuery)) {
    return 2;
  }
  return normalizedLabel.includes(normalizedQuery) ? 3 : null;
}

export interface WaterEntityLabelSource {
  id: string;
  name: string;
  department?: string;
  commune?: string;
}

/**
 * Libellés des milieux d'une liste de résultats (#230, même règle que
 * fishola-mobile Helpers.waterEntityLabels) : les homonymes (même nom à la
 * casse et aux accents près) sont suffixés de leur département, « La Bourbre
 * (38) », et de la commune s'ils partagent le même département, « Étang Neuf
 * (01 – Bourg-en-Bresse) ». Sans département connu, ou sans homonyme, le nom
 * seul. Renvoie le libellé par id de milieu.
 */
export function waterEntityLabels(entities: WaterEntityLabelSource[]): Map<string, string> {
  const uniqueById = new Map(entities.map((entity) => [entity.id, entity]));
  const homonymsByName = new Map<string, WaterEntityLabelSource[]>();
  uniqueById.forEach((entity) => {
    const key = (entity.name || "").normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().trim();
    homonymsByName.set(key, [...(homonymsByName.get(key) || []), entity]);
  });
  const labels = new Map<string, string>();
  homonymsByName.forEach((homonyms) => {
    homonyms.forEach((entity) => labels.set(entity.id, homonymLabel(entity, homonyms)));
  });
  return labels;
}

function homonymLabel(entity: WaterEntityLabelSource, homonyms: WaterEntityLabelSource[]): string {
  if (homonyms.length < 2 || !entity.department) {
    return entity.name;
  }
  const sameDepartment = homonyms.some(
    (other) => other.id !== entity.id && other.department === entity.department
  );
  const suffix = sameDepartment && entity.commune ? `${entity.department} – ${entity.commune}` : entity.department;
  return `${entity.name} (${suffix})`;
}
