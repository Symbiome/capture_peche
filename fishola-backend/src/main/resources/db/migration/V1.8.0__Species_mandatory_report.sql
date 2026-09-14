-- Déclaration obligatoire par espèce (#91). Après validation d'une prise d'une
-- espèce soumise à déclaration, l'app pêcheur affiche une alerte avec un lien
-- vers le formulaire officiel de déclaration (ou un message générique si aucun
-- lien n'est renseigné).
ALTER TABLE public.species ADD COLUMN mandatory_report boolean NOT NULL DEFAULT false;
ALTER TABLE public.species ADD COLUMN report_link text;

COMMENT ON COLUMN public.species.mandatory_report IS 'Espèce soumise à déclaration obligatoire (#91)';
COMMENT ON COLUMN public.species.report_link IS 'Lien vers le formulaire officiel de déclaration, NULL si non renseigné (#91)';
