-- Référentiel taxonomique des espèces (#92). L'UFBRMC fournit ce référentiel au
-- format SANDRE/TAXREF (fichier « Liste sp v1.xlsx » joint à l'issue), PAS au
-- format Darwin Core initialement envisagé dans la user story : on stocke donc
-- les colonnes telles que définies par ce fichier plutôt que genus/family/etc.
--
-- code_espece : code SANDRE 3 lettres de l'espèce (ex. « TRF »), identifiant
-- stable du référentiel, utilisé par scripts/species_taxon_seed.sql pour
-- rattacher/mettre à jour une espèce sans dépendre de son libellé.
ALTER TABLE public.species ADD COLUMN code_espece character varying(3);
ALTER TABLE public.species ADD COLUMN code_taxon_sandre integer;
ALTER TABLE public.species ADD COLUMN code_taxref integer;
ALTER TABLE public.species ADD COLUMN scientific_name text;

CREATE UNIQUE INDEX species_code_espece_key ON public.species (code_espece) WHERE code_espece IS NOT NULL;

COMMENT ON COLUMN public.species.code_espece IS 'Code SANDRE 3 lettres de l''espèce (référentiel UFBRMC, #92)';
COMMENT ON COLUMN public.species.code_taxon_sandre IS 'Code taxon SANDRE (référentiel UFBRMC, #92)';
COMMENT ON COLUMN public.species.code_taxref IS 'Identifiant TAXREF (INPN/GBIF), NULL si non disponible (#92)';
COMMENT ON COLUMN public.species.scientific_name IS 'Nom scientifique SANDRE (nom binomial), affiché en secondaire sous le nom usuel (#92)';

-- Bornes de masse aberrante par espèce (pendant de min_size_cm/max_size_cm
-- posées en V1.2.0, jusqu'ici vides) : mêmes unités que catch.weight (grammes).
ALTER TABLE public.species_size_bounds ADD COLUMN min_weight_g integer;
ALTER TABLE public.species_size_bounds ADD COLUMN max_weight_g integer;

COMMENT ON COLUMN public.species_size_bounds.min_weight_g IS 'Masse minimale plausible (g), référentiel UFBRMC (#92)';
COMMENT ON COLUMN public.species_size_bounds.max_weight_g IS 'Masse maximale plausible (g), référentiel UFBRMC (#92)';
