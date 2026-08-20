-- Code postal principal de la commune (#15, désambiguïsation à la saisie).
-- Distinct du code INSEE (insee_com) ; source : geo.api.gouv.fr / La Poste.
-- Une commune peut avoir plusieurs CP : on ne stocke que le principal (affichage).
ALTER TABLE public.commune ADD COLUMN IF NOT EXISTS code_postal character varying(5);
COMMENT ON COLUMN public.commune.code_postal IS 'Code postal principal (affichage) ; NULL si non renseigné. Distinct de insee_com (code INSEE).';
