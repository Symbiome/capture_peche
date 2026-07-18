"""
Django settings — backend « gestion interne » (capture_peche / Symbiome).

Ce backend porte le module administrateur (Phase 1 §3.1) et le module de saisie
opérateur (§3.3). Il partage la BDD applicative Fishola (PostgreSQL/PostGIS) :
Flyway/Fishola reste l'autorité du schéma métier ; les modèles Django sur ces
tables sont `managed = False`. Django ne gère que ses propres tables (auth, admin,
sessions, contenttypes).
"""
from pathlib import Path
import os

from django.urls import reverse_lazy
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent.parent
load_dotenv(BASE_DIR / ".env")


def _env_bool(name: str, default: bool = False) -> bool:
    return os.environ.get(name, str(default)).lower() in {"1", "true", "yes", "on"}


SECRET_KEY = os.environ.get("DJANGO_SECRET_KEY", "dev-insecure-change-me")
DEBUG = _env_bool("DJANGO_DEBUG", True)
ALLOWED_HOSTS = [
    h.strip()
    for h in os.environ.get("DJANGO_ALLOWED_HOSTS", "localhost,127.0.0.1").split(",")
    if h.strip()
]

INSTALLED_APPS = [
    # django-unfold (thème d'admin « AquaAdmin ») : doit précéder django.contrib.admin.
    "unfold",
    "unfold.contrib.filters",
    "unfold.contrib.forms",
    "unfold.contrib.import_export",
    "django.contrib.admin",
    "django.contrib.auth",
    "django.contrib.contenttypes",
    "django.contrib.sessions",
    "django.contrib.messages",
    "django.contrib.staticfiles",
    "django.contrib.postgres",  # lookup __unaccent (résolution du référentiel à l'import)
    "import_export",
    "backoffice",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "django.contrib.sessions.middleware.SessionMiddleware",
    "django.middleware.common.CommonMiddleware",
    "django.middleware.csrf.CsrfViewMiddleware",
    "django.contrib.auth.middleware.AuthenticationMiddleware",
    "django.contrib.messages.middleware.MessageMiddleware",
    "django.middleware.clickjacking.XFrameOptionsMiddleware",
]

ROOT_URLCONF = "config.urls"

TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {
            "context_processors": [
                "django.template.context_processors.request",
                "django.contrib.auth.context_processors.auth",
                "django.contrib.messages.context_processors.messages",
            ],
        },
    },
]

WSGI_APPLICATION = "config.wsgi.application"

# BDD applicative Fishola PARTAGÉE. Le schéma est géré par Flyway côté Fishola ;
# ici les modèles métier sont `managed = False` (cf. backoffice/models.py).
DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.postgresql",
        "NAME": os.environ.get("DB_NAME", "fishola"),
        "USER": os.environ.get("DB_USER", "postgres"),
        "PASSWORD": os.environ.get("DB_PASSWORD", "whatever"),
        "HOST": os.environ.get("DB_HOST", "localhost"),
        "PORT": os.environ.get("DB_PORT", "15432"),
    }
}

AUTH_PASSWORD_VALIDATORS = [
    {"NAME": "django.contrib.auth.password_validation.UserAttributeSimilarityValidator"},
    {"NAME": "django.contrib.auth.password_validation.MinimumLengthValidator"},
    {"NAME": "django.contrib.auth.password_validation.CommonPasswordValidator"},
    {"NAME": "django.contrib.auth.password_validation.NumericPasswordValidator"},
]

# L'admin est la seule interface : on y renvoie après connexion (pas de /accounts/profile/).
LOGIN_REDIRECT_URL = "/admin/"

LANGUAGE_CODE = "fr-fr"
TIME_ZONE = "Europe/Paris"
USE_I18N = True
USE_TZ = True

STATIC_URL = "static/"
STATIC_ROOT = BASE_DIR / "staticfiles"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"


# --- Thème d'admin « AquaAdmin » (django-unfold) ----------------------------
# Reprend la maquette du mémoire (§3.1) : en-tête AquaAdmin + navigation
# Tableau de bord / Utilisateurs / Profils & Droits / Historique + données de pêche.
UNFOLD = {
    "SITE_TITLE": "AquaAdmin",
    "SITE_HEADER": "AquaAdmin",
    "SITE_SUBHEADER": "Gestion des Données de Pêche",
    "SITE_SYMBOL": "phishing",  # icône Material Symbols (thème pêche)
    "SITE_URL": None,  # pas de site public → masque « Voir le site » / « Return to site »
    "SHOW_HISTORY": True,
    "SHOW_VIEW_ON_SITE": False,
    "COLORS": {
        # Palette « aqua » (bleu) pour coller au thème.
        "primary": {
            "50": "239 246 255", "100": "219 234 254", "200": "191 219 254",
            "300": "147 197 253", "400": "96 165 250", "500": "59 130 246",
            "600": "37 99 235", "700": "29 78 216", "800": "30 64 175",
            "900": "30 58 138", "950": "23 37 84",
        },
    },
    "SIDEBAR": {
        "show_search": True,
        "navigation": [
            {
                "title": "Administration",
                "items": [
                    {"title": "Tableau de bord", "icon": "dashboard",
                     "link": reverse_lazy("admin:index")},
                    {"title": "Utilisateurs", "icon": "person",
                     "link": reverse_lazy("admin:auth_user_changelist")},
                    {"title": "Profils & Droits", "icon": "shield_person",
                     "link": reverse_lazy("admin:auth_group_changelist")},
                    {"title": "Historique", "icon": "history",
                     "link": reverse_lazy("admin:backoffice_auditlog_changelist")},
                ],
            },
            {
                "title": "Données de pêche",
                "items": [
                    {"title": "Sorties", "icon": "sailing",
                     "link": reverse_lazy("admin:backoffice_trip_changelist")},
                    {"title": "Imports", "icon": "upload_file",
                     "link": reverse_lazy("admin:backoffice_importjob_changelist")},
                    {"title": "Espèces", "icon": "set_meal",
                     "link": reverse_lazy("admin:backoffice_species_changelist")},
                    {"title": "Entités hydro", "icon": "water",
                     "link": reverse_lazy("admin:backoffice_waterentity_changelist")},
                    {"title": "Communes", "icon": "location_city",
                     "link": reverse_lazy("admin:backoffice_commune_changelist")},
                ],
            },
        ],
    },
}
