"""URLs du backend « gestion interne ».

L'admin Django (thémé « AquaAdmin » via django-unfold, cf. settings `UNFOLD`) est
le cœur de l'interface. La racine `/` **redirige vers l'admin** :
ce backend est purement back-office — il n'a pas de site public (celui-ci est
porté par Fishola, l'autre backend).
"""
from django.contrib import admin
from django.urls import path
from django.views.generic import RedirectView

from backoffice import maps

# Pas de site public : on retire les liens « Voir le site » (menu admin) et
# « Return to site » (page de connexion).
admin.site.site_url = None

urlpatterns = [
    path("", RedirectView.as_view(pattern_name="admin:index", permanent=False)),
    # Vue « Carte » (consultation seule) — montée sous /admin/ pour partager la
    # session et le chrome de l'admin ; déclarée AVANT admin.site.urls.
    path("admin/carte/", maps.carte_view, name="carte"),
    path("admin/carte/sessions.geojson", maps.sessions_geojson, name="carte_sessions"),
    # Tuiles hydro : servies par l'endpoint MVT public de Quarkus (source unique) —
    # pas d'endpoint de tuiles côté back-office (cf. maps._hydro_tiles_url).
    # Recherches (parité mobile #6/#7) — servent la sélection de l'entité hydro.
    path("admin/carte/search/entities", maps.waterentity_search, name="hydro_search_entities"),
    path("admin/carte/search/communes", maps.commune_search, name="hydro_search_communes"),
    path("admin/carte/search/by-commune", maps.waterentities_by_commune, name="hydro_by_commune"),
    path("admin/carte/entity", maps.waterentity_detail, name="hydro_entity_detail"),
    path("admin/backoffice/catch/<uuid:catch_id>/photo/<int:index>/",
         maps.catch_photo, name="catch_photo"),
    path("admin/", admin.site.urls),
]
