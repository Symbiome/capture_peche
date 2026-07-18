"""Vue « Carte » du back-office (consultation seule).

Reprend le concept de la carte pêcheur (MapLibre + fonds IGN Géoplateforme +
réseau hydrographique en tuiles vectorielles) pour **visualiser les sessions**
sur une carte, avec une liste latérale. Un clic sur une session ouvre sa fiche
(en lecture seule). Aucune écriture ici.

Le réseau hydrographique est produit **directement ici** en tuiles vectorielles
MVT (ST_AsMVT) à partir de la base partagée — même logique que la carte pêcheur,
sans dépendre du backend Fishola.
"""
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


# Tuile vectorielle (MVT) du réseau hydro pour une tuile slippy-map z/x/y : deux
# couches (river_section : lignes ; water_surface : polygones), comme la carte
# pêcheur (#8). Préfiltre bbox en 4326 (index GIST), ST_AsMVTGeom en 3857.
_HYDRO_TILE_SQL = """
    WITH env AS (SELECT ST_TileEnvelope(%s, %s, %s) AS g),
    rs AS (
      SELECT ST_AsMVTGeom(ST_Transform(r.geom, 3857), env.g, 4096, 64, true) AS geom,
             r.water_entity_id::text AS water_entity_id, we.name AS name, r.persistent AS persistent
      FROM river_section r JOIN water_entity we ON we.id = r.water_entity_id, env
      WHERE r.geom && ST_Transform(env.g, 4326)
    ),
    ws AS (
      SELECT ST_AsMVTGeom(ST_Transform(s.geom, 3857), env.g, 4096, 64, true) AS geom,
             s.water_entity_id::text AS water_entity_id, we.name AS name
      FROM water_surface s JOIN water_entity we ON we.id = s.water_entity_id, env
      WHERE s.geom && ST_Transform(env.g, 4326)
    )
    SELECT coalesce((SELECT ST_AsMVT(rs.*, 'river_section') FROM rs WHERE geom IS NOT NULL), ''::bytea)
        || coalesce((SELECT ST_AsMVT(ws.*, 'water_surface')  FROM ws WHERE geom IS NOT NULL), ''::bytea) AS tile
"""


def _hydro_tiles_url() -> str:
    """URL-modèle (absolue) des tuiles hydro servies par ce back-office."""
    return "/admin/carte/tiles/hydro/{z}/{x}/{y}.pbf"


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


@staff_member_required
def hydro_tile(request, z, x, y):
    """Tuile vectorielle (MVT) du réseau hydro, produite depuis la base partagée."""
    with connection.cursor() as cursor:
        cursor.execute(_HYDRO_TILE_SQL, [z, x, y])
        row = cursor.fetchone()
    data = bytes(row[0]) if row and row[0] is not None else b""
    return HttpResponse(data, content_type="application/x-protobuf")
