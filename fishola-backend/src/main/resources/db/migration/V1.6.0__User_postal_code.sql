-- Code postal du pêcheur, saisi à l'inscription (#94), pour permettre aux
-- fédérations d'analyser les données par zone géographique. `birth_year`
-- existe déjà sur fishola_user depuis le schéma initial et sert au même usage
-- pour la tranche d'âge ; seul le code postal manque.
--
-- Colonne NULLable : les comptes existants n'ont pas ce champ et ne sont pas
-- rétroactivement contraints. L'obligation ne s'applique qu'à l'inscription,
-- au niveau applicatif (SecurityResource#register).
ALTER TABLE public.fishola_user ADD COLUMN postal_code character varying(5);
COMMENT ON COLUMN public.fishola_user.postal_code IS 'Code postal du pêcheur (5 chiffres) ; NULL pour les comptes créés avant #94';
