-- Ajoute une colonne pour distinguer les utilisateurs impliqués dans le développement du projet
ALTER TABLE fishola_user ADD COLUMN exclude_from_exports BOOLEAN NOT NULL DEFAULT FALSE;

-- Par défaut on ignore tous les comptes utilisateurs en 'codelutin.com'
UPDATE fishola_user SET exclude_from_exports = TRUE WHERE email LIKE '%codelutin.com';
