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

// Minuscules, sans accents, tirets et apostrophes remplacés par des espaces :
// « Saône » et « saone », « Chalon-sur-Saône » et « chalon sur saone » se valent.
export function normalizeSearchText(value: string): string {
  return (value || "")
    .normalize("NFD")
    .replace(/\p{M}/gu, "")
    .toLowerCase()
    .replace(/[-'’]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
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
