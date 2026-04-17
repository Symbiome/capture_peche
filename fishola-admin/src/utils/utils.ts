export function showLink(event: Event, url: string) {
  // Do not foward click event to row (would trigger modal)
  event.stopPropagation();

  window.open(url, "_blank");
}
