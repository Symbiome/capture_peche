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
