"""URLs du backend « gestion interne ». Le module d'administration (Django Admin,
personnalisé « AquaAdmin ») est le cœur de l'interface (maquette mémoire §3.1)."""
from django.contrib import admin
from django.urls import path

# Personnalisation « AquaAdmin » (maquette mémoire technique §3.1).
admin.site.site_header = "AquaAdmin — Gestion des Données de Pêche"
admin.site.site_title = "AquaAdmin"
admin.site.index_title = "Administration"

urlpatterns = [
    path("admin/", admin.site.urls),
]
