"""URLs du backend « gestion interne ».

L'admin Django (thémé « AquaAdmin » via django-unfold, cf. settings `UNFOLD`) est
le cœur de l'interface (maquette §3.1). La racine `/` **redirige vers l'admin** :
ce backend est purement back-office — il n'a pas de site public (celui-ci est
porté par Fishola, l'autre backend).
"""
from django.contrib import admin
from django.urls import path
from django.views.generic import RedirectView

urlpatterns = [
    path("", RedirectView.as_view(pattern_name="admin:index", permanent=False)),
    path("admin/", admin.site.urls),
]
