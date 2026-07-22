"""Vue « Carte » du back-office (consultation seule).

Reprend le concept de la carte pêcheur (MapLibre + fonds IGN Géoplateforme +
réseau hydrographique en tuiles vectorielles) pour **visualiser les sessions**
sur une carte, avec une liste latérale. Un clic sur une session ouvre sa fiche
(en lecture seule). Aucune écriture ici.

Le réseau hydrographique est produit **directement ici** en tuiles vectorielles
MVT (ST_AsMVT) à partir de la base partagée — même logique que la carte pêcheur,
sans dépendre du backend Fishola.
"""
from django.conf import settings
from django.contrib import admin
from django.contrib.admin.views.decorators import staff_member_required
from django.db import connection
from django.http import HttpResponse, JsonResponse
from django.template.response import TemplateResponse

# Coordonnées d'une session : on privilégie le centroïde de l'entité hydro
# (colonnes lon/lat déjà en WGS84) et on retombe sur la position de départ.
_SESSIONS_SQL = """
    SELECT t.id,
           t.name,
           t.day,
           we.name AS water_entity_name,
           t.collection_method,
           t.source,
           COALESCE(we.longitude, ST_X(ST_Transform(t.begin_position, 4326))) AS lon,
           COALESCE(we.latitude,  ST_Y(ST_Transform(t.begin_position, 4326))) AS lat,
           (SELECT count(*) FROM catch c WHERE c.trip_id = t.id) AS catch_count
    FROM trip t
    LEFT JOIN water_entity we ON we.id = t.water_entity_id
    WHERE COALESCE(we.longitude, ST_X(ST_Transform(t.begin_position, 4326))) IS NOT NULL
    ORDER BY t.day DESC NULLS LAST
    LIMIT 2000
"""


def _hydro_tiles_url() -> str:
    """URL-modèle des tuiles hydro. **Source unique = l'endpoint MVT public de
    Quarkus** (`/api/v1/tiles/hydro/...`) : le back-office ne duplique pas la
    génération de tuiles. CORS Quarkus autorise déjà l'origine du back-office ;
    la base est surchargeable via `FISHOLA_API_BASE_URL` (prod)."""
    return f"{settings.FISHOLA_API_BASE_URL}/api/v1/tiles/hydro/{{z}}/{{x}}/{{y}}.pbf"


@staff_member_required
def carte_view(request):
    context = {
        **admin.site.each_context(request),
        "title": "Carte des sessions",
        "hydro_tiles_url": _hydro_tiles_url(),
    }
    return TemplateResponse(request, "admin/backoffice/carte.html", context)


@staff_member_required
def sessions_geojson(request):
    """Sessions géolocalisées en FeatureCollection GeoJSON."""
    with connection.cursor() as cursor:
        cursor.execute(_SESSIONS_SQL)
        columns = [c[0] for c in cursor.description]
        rows = [dict(zip(columns, r)) for r in cursor.fetchall()]

    features = []
    for r in rows:
        features.append({
            "type": "Feature",
            "geometry": {"type": "Point", "coordinates": [float(r["lon"]), float(r["lat"])]},
            "properties": {
                "id": str(r["id"]),
                "name": r["name"] or "Sortie",
                "day": r["day"].isoformat() if r["day"] else None,
                "water_entity": r["water_entity_name"],
                "collection_method": r["collection_method"],
                "source": r["source"],
                "catch_count": r["catch_count"],
            },
        })
    return JsonResponse({"type": "FeatureCollection", "features": features})


@staff_member_required
def catch_photo(request, catch_id, index):
    """Sert une photo de capture (stockée en base, `catch_picture.content`)."""
    with connection.cursor() as cursor:
        cursor.execute(
            "SELECT content FROM catch_picture WHERE catch_id = %s AND picture_index = %s",
            [str(catch_id), index],
        )
        row = cursor.fetchone()
    if not row or not row[0]:
        return HttpResponse(status=404)
    data = bytes(row[0])
    content_type = "image/png" if data[:8] == b"\x89PNG\r\n\x1a\n" else "image/jpeg"
    return HttpResponse(data, content_type=content_type)


# --- Recherches (parité mobile #6/#7) : nom d'entité, commune, byCommune -----
# SQL porté de HydroSearchDao (Quarkus) : accent-insensible (f_unaccent) +
# typo-tolérant (pg_trgm), tri préfixe → similarité → nom. Sert la sélection
# de l'entité hydro sans dépendre du rendu des tuiles.

def _for_search(q):
    """Neutralise les métacaractères LIKE / trigramme (comme `forSearch` côté back)."""
    return (q or "").translate({ord(c): None for c in "%_\\"}).strip()


_SEARCH_ENTITIES_SQL = """
    SELECT we.id::text AS id, we.name, we.kind, we.latitude AS lat, we.longitude AS lng
    FROM water_entity we
    WHERE we.geom IS NOT NULL
      AND (f_unaccent(we.name) ILIKE '%%' || f_unaccent(%(q)s) || '%%'
           OR f_unaccent(we.name) %% f_unaccent(%(q)s))
    ORDER BY (f_unaccent(we.name) ILIKE f_unaccent(%(q)s) || '%%') DESC,
             similarity(f_unaccent(we.name), f_unaccent(%(q)s)) DESC, we.name
    LIMIT 15
"""

_SEARCH_COMMUNES_SQL = """
    SELECT c.insee_com AS insee, c.name, c.latitude AS lat, c.longitude AS lng
    FROM commune c
    WHERE f_unaccent(c.name) ILIKE '%%' || f_unaccent(%(q)s) || '%%'
       OR f_unaccent(c.name) %% f_unaccent(%(q)s)
    ORDER BY (f_unaccent(c.name) ILIKE f_unaccent(%(q)s) || '%%') DESC,
             similarity(f_unaccent(c.name), f_unaccent(%(q)s)) DESC, c.name
    LIMIT 10
"""

# Entités hydro d'une commune : celles à moins de `bufferM` de sa limite,
# triées par distance au centroïde communal (geography-cast, index GIST geog).
_BY_COMMUNE_SQL = """
    SELECT DISTINCT ON (we.id)
           we.id::text AS id, we.name, we.kind, we.latitude AS lat, we.longitude AS lng
    FROM commune c
    JOIN water_entity we
      ON ST_DWithin(we.geom::geography, c.geom::geography, %(buffer)s)
    WHERE c.insee_com = %(insee)s AND we.geom IS NOT NULL
    ORDER BY we.id, ST_Distance(we.geom::geography, ST_Centroid(c.geom)::geography)
    LIMIT 100
"""


def _rows_as_dicts(cursor):
    cols = [c[0] for c in cursor.description]
    return [dict(zip(cols, r)) for r in cursor.fetchall()]


@staff_member_required
def waterentity_search(request):
    """Recherche d'entités hydro par nom (#7). `?q=` ≥ 2 caractères."""
    q = _for_search(request.GET.get("q", ""))
    if len(q) < 2:
        return JsonResponse({"results": []})
    with connection.cursor() as cursor:
        cursor.execute(_SEARCH_ENTITIES_SQL, {"q": q})
        results = _rows_as_dicts(cursor)
    return JsonResponse({"results": results})


@staff_member_required
def commune_search(request):
    """Recherche de communes par nom (#6). `?q=` ≥ 2 caractères."""
    q = _for_search(request.GET.get("q", ""))
    if len(q) < 2:
        return JsonResponse({"results": []})
    with connection.cursor() as cursor:
        cursor.execute(_SEARCH_COMMUNES_SQL, {"q": q})
        results = _rows_as_dicts(cursor)
    return JsonResponse({"results": results})


@staff_member_required
def waterentities_by_commune(request):
    """Entités hydro d'une commune (#6). `?insee=` requis, `?bufferM=` (défaut 500)."""
    insee = (request.GET.get("insee", "") or "").strip()
    if not insee:
        return JsonResponse({"results": []})
    try:
        buffer_m = min(max(int(request.GET.get("bufferM", 500)), 0), 20000)
    except ValueError:
        buffer_m = 500
    with connection.cursor() as cursor:
        cursor.execute(_BY_COMMUNE_SQL, {"insee": insee, "buffer": buffer_m})
        results = _rows_as_dicts(cursor)
    return JsonResponse({"results": results})
