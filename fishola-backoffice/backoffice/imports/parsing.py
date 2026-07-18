"""Lecture du CSV et coercition des valeurs (séparateur « ; », UTF-8)."""
import csv
import datetime as dt
import io

DELIMITER = ";"


def read_rows(text):
    """Retourne (header, records) ; records = liste de dict colonne -> valeur (strippée)."""
    reader = csv.reader(io.StringIO(text), delimiter=DELIMITER)
    rows = list(reader)
    if not rows:
        return [], []
    header = [h.strip() for h in rows[0]]
    records = []
    for raw in rows[1:]:
        if not any((cell or "").strip() for cell in raw):
            continue  # ligne entièrement vide
        record = {
            header[i]: (raw[i].strip() if i < len(raw) else "")
            for i in range(len(header))
        }
        records.append(record)
    return header, records


def parse_date(value):
    return dt.datetime.strptime(value.strip(), "%d/%m/%Y").date()


def parse_time(value):
    return dt.datetime.strptime(value.strip(), "%H:%M").time()


def parse_int(value):
    return int(value.strip())


def parse_bool_ouinon(value):
    v = (value or "").strip().lower()
    if v in {"oui", "o", "true", "1"}:
        return True
    if v in {"non", "n", "false", "0"}:
        return False
    raise ValueError(f"valeur oui/non attendue, reçu « {value} »")


def is_blank(value):
    return value is None or str(value).strip() == ""
