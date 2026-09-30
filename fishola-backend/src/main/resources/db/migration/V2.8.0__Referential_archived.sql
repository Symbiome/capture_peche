-- Archivage (suppression logique) des espèces et techniques de pêche (#202) :
-- un élément déjà utilisé par des captures ou des sorties ne peut pas être
-- supprimé sans perdre l'historique. Archivé, il n'est plus proposé à la
-- saisie mais reste lisible sur les données existantes.
ALTER TABLE species ADD COLUMN archived boolean NOT NULL DEFAULT false;
ALTER TABLE technique ADD COLUMN archived boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN species.archived IS 'Espèce retirée des listes de saisie, conservée pour l''historique (#202)';
COMMENT ON COLUMN technique.archived IS 'Technique retirée des listes de saisie, conservée pour l''historique (#202)';
