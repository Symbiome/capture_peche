-- Charge le référentiel taxonomique des espèces fourni par l'UFBRMC (#92),
-- format SANDRE/TAXREF défini par le fichier « Liste sp v1.xlsx » joint à
-- l'issue (PAS le format Darwin Core initialement envisagé). Idempotent et
-- rejouable : rattache/actualise les espèces déjà en base et insère celles
-- qui manquent.
--
-- Usage : psql "$DATABASE_URL" -f scripts/species_taxon_seed.sql
-- (nécessite le schéma V1.7.0__Species_taxon_reference.sql).
--
-- Unités du fichier source : taille en mm, masse en g. Les colonnes
-- species_size_bounds.min_size_cm/max_size_cm sont en cm (convention déjà en
-- place dans le backend, cf. ImportService#sizeOutOfBounds / capture_longueur_cm) :
-- conversion mm -> cm par arrondi vers l'extérieur (floor pour le minimum,
-- ceil pour le maximum) afin de ne pas resserrer les bornes réelles.
--
-- Deux libellés usuels du fichier source sont en doublon (contrainte
-- d'unicité sur species.name/export_as) et corrigés ici pour rester
-- distincts, toutes les autres colonnes étant reprises telles quelles :
--  - SDF (Salvelinus fontinalis) : « Omble chevalier » -> « Omble de fontaine »
--    (nom vernaculaire usuel de cette espèce ; OBL/Salvelinus umbla garde
--    « Omble chevalier »).
--  - HOX (Chondrostoma nasus x Parachondrostoma toxostoma, cf. son propre nom
--    scientifique double) : « Toxostome » -> « Toxostome x Hotu » (hybride ;
--    TOX/Parachondrostoma toxostoma garde « Toxostome »).

BEGIN;

CREATE TEMP TABLE species_taxon_staging (
    code_espece character varying(3),
    code_taxon_sandre integer,
    masse_min_g numeric,
    masse_max_g numeric,
    taille_min_mm numeric,
    taille_max_mm numeric,
    code_taxref integer,
    nom_scientifique_sandre text,
    nom_usuel text
) ON COMMIT DROP;

INSERT INTO species_taxon_staging
    (code_espece, code_taxon_sandre, masse_min_g, masse_max_g, taille_min_mm, taille_max_mm, code_taxref, nom_scientifique_sandre, nom_usuel)
VALUES
('ABL', 2090, 1, 135, 24, 324, 67111, 'Alburnus alburnus', 'Ablette'),
('BBG', 2053, 1, 300, 138, 249, 69346, 'Micropterus salmoides', 'Black-bass'),
('ALF', 2057, 1, 1, 38, 537, 66996, 'Alosa fallax', 'Alose feinte atlantique'),
('ALR', 2058, NULL, NULL, 122, 435, 67003, 'Alosa fallax rhodanensis', 'Alose feinte du Rhône'),
('CTI', 31039, 1, 1000, 800, 1200, 67246, 'Ctenopharyngodon idella', 'Amour blanc, Carpe amour'),
('ANG', 2038, 1, 1080.5, 68, 690, 66832, 'Anguilla anguilla', 'Anguille européenne'),
('APR', 2197, 10, 11, 40, 180, 69378, 'Zingel asper', 'Apron du Rhône'),
('ASP', 68136, 108, 108, 46, 162, 200255, 'Leuciscus aspius', 'Aspe'),
('LOU', 2234, 25, 36, 34, 39, 69317, 'Dicentrarchus labrax', 'Bar'),
('BAF', 2096, 1, 3600, 30, 280, 67143, 'Barbus barbus', 'Barbeau fluviatile'),
('BAM', 2097, 1, 260, 60, 136, 67179, 'Barbus meridionalis', 'Barbeau méridional'),
('BLN', 25609, 1, 4883, 24, 192, 67335, 'Telestes souffia', 'Blageon'),
('BLE', 2045, 2, 2, 40, 105, 70014, 'Salaria fluviatilis', 'Blennie fluviatile'),
('BOU', 2131, 1, 642, 39, 45, 67420, 'Rhodeus amarus', 'Bouvière'),
('BRE', 2086, 32, 78, 36, 524, 67074, 'Abramis brama', 'Brème commune'),
('BRO', 2151, 1.5, 9140, 450, 450, 67606, 'Esox lucius', 'Brochet'),
('CAX', 2100, 4, 1570, 35, 78, 190332, 'Carassius', 'Carassin commun'),
('CCO', 2110, 4, 5000, 46, 815, NULL, 'Cyprinus carpio', 'Carpe commune'),
('CHX', 2079, 1, 125, 11, 277, 191213, 'Cottus', 'Chabot'),
('CHE', 31041, 1, 6565, 14, 236, 67310, 'Squalius cephalus', 'Chevesne'),
('COR', 2076, 460, 460, 125, 176, 191138, 'Coregonus', 'Corégone'),
('CDR', 2048, 4, 10, 7, 263, 69336, 'Ambloplites rupestris', 'Crapet de roche'),
('CRI', 2228, 28, 28, 166, 790, 67819, 'Salvelinus namaycush', 'Cristivomer'),
('ASL', 2963, 14, 14, 28, 148, 162666, 'Astacus leptodactylus', 'Ecrevisse à pattes grêles'),
('ASA', 866, 11.5, 115, 95, 175, 18432, 'Astacus astacus', 'Ecrevisse à pattes rouges'),
('OCL', 68137, 1, 12, 19, 93, 853999, 'Faxonius limosus', 'Ecrevisse américaine'),
('PFL', 873, 1, 52, 30, 138, 162667, 'Pacifastacus leniusculus', 'Ecrevisse de Californie'),
('PCC', 2028, 1, 81, 50, 140, 162668, 'Procambarus clarkii', 'Ecrevisse de Louisiane'),
('APP', 868, 1, 322, 21, 100, 18437, 'Austropotamobius pallipes', 'Ecrevisses à pattes blanches'),
('EPI', 2165, 1, 87, 4, 261, 426026, 'Gasterosteus aculeatus aculeatus', 'Épinoche à trois épines'),
('PAP', 2543, 40, 1268, 14, 174, 199188, 'Pachychilon pictum', 'Épirine lippue'),
('EST', 2032, 1, 1, NULL, NULL, 66775, 'Acipenser sturio', 'Esturgeon de l''Adriatique'),
('GAM', 2208, 1, 2, 20, 55, 68825, 'Gambusia affinis', 'Gambusie'),
('GAR', 2133, 1, 1675, 12, 270, 67422, 'Rutilus rutilus', 'Gardon'),
('GTN', 29117, 1, 2700, 2, 135, 70155, 'Neogobius melanostomus', 'Gobie à tâche noire'),
('GFL', 36288, NULL, NULL, 35, 106, NULL, 'Neogobius fluviatilis', 'Gobie fluviatile'),
('GBN', 2172, 1, 1108, 49, 110, 70142, 'Gobius niger', 'Gobie noir'),
('GOU', 2113, 4, 605, 40, 161, 67257, 'Gobio gobio', 'Goujon'),
('ALA', 2056, 1, 1, 40, 624, 66967, 'Alosa alosa', 'Grande alose'),
('GRE', 2191, 5, 341, 3, 130, 69354, 'Gymnocephalus cernuus', 'Grémille'),
('HOT', 2104, 2, 309, 80, 125, 67220, 'Chondrostoma nasus', 'Hotu'),
('IDE', 2121, 42, 229, 29, 48, 67304, 'Leuciscus idus', 'Ide mélanote'),
('ATB', 31701, 2, 2, 65, 85, 386340, 'Atherina (Hepsetia) boyeri', 'Joël, Athérine'),
('ABH', 2117, 1, 5, 40, 50, 67286, 'Leucaspius delineatus', 'L''Able de Heckel'),
('LPP', 2012, 1, 28, 35, 114, 66333, 'Lampetra planeri', 'Lamproie de Planer'),
('LPR', 2011, 164, 164, 64, 354, 66330, 'Lampetra fluviatilis', 'Lamproie fluviatile'),
('LPM', 2014, 1, 820, 143, 158, 66315, 'Petromyzon marinus', 'Lamproie marine'),
('LOX', 5073, 1, 2662, 51, 76, 189745, 'Barbatula', 'Loche'),
('LOR', 2067, 4, 7, 57, 124, 67506, 'Cobitis taenia', 'Loche de rivière'),
('LOE', 2069, 18, 115, 20, 280, 67534, 'Misgurnus fossilis', 'Loche d''étang'),
('LOT', 2156, 380, 770, 5, 875, 68336, 'Lota lota', 'Lote'),
('MUC', 2185, 1, 1838, 58, 187, NULL, 'Mugil cephalus', 'Mulet à grosse tête, Mulet cabot'),
('MGL', 2180, 2, 27, 72, 91, 69777, 'Chelon labrosus', 'Mulet lippu'),
('OBL', 32267, NULL, NULL, 42, 368, 67837, 'Salvelinus umbla', 'Omble chevalier'),
('SDF', 2227, 60, 138, 75, 410, 67817, 'Salvelinus fontinalis', 'Omble de fontaine'),
('OBR', 2247, 112, 112, 60, 404, 67759, 'Thymallus thymallus', 'Ombre commun'),
('PER', 2193, 95, 301, 15, 310, 69350, 'Perca fluviatilis', 'Perche'),
('PES', 2050, 1, 18, 21, 125, 69338, 'Lepomis gibbosus', 'Perche-soleil'),
('PCH', 2177, 7, 195, 35, 240, 67571, 'Ameiurus melas', 'Poisson-chat'),
('PSR', 2129, 3, 3, 16, 91, 67415, 'Pseudorasbora parva', 'Pseudorasbora, Goujon asiatique'),
('ROT', 2135, 1, 328, 86, 150, 67466, 'Scardinius erythrophthalmus', 'Rotengle'),
('SAN', 2195, 45, 225, 32, 125, 69372, 'Sander lucioperca', 'Sandre'),
('SAT', 2220, 17, 20, 200, 400, 67765, 'Salmo salar', 'Saumon atlantique'),
('SIL', 2238, 64, 64, 300, 530, 67585, 'Silurus glanis', 'Silure glane'),
('SPI', 2088, 1, 6672, 28, 139, 67104, 'Alburnoides bipunctatus', 'Spirlin'),
('TAN', 2137, 63, 63, 45, 249, 67478, 'Tinca tinca', 'Tanche'),
('PIM', 2127, NULL, NULL, 42, 77, 67401, 'Pimephales promelas', 'Tête de boule, Vairon canadien'),
('HOX', 31680, NULL, NULL, 62, 115, NULL, 'Chondrostoma nasus, Parachondrostoma toxostoma', 'Toxostome x Hotu'),
('TOX', 31135, 13, 9312, 4, 1591, 458701, 'Parachondrostoma toxostoma', 'Toxostome, Sofie'),
('TAC', 2216, 10, 2500, 380, 380, 67804, 'Oncorhynchus mykiss', 'Truite arc-en-ciel'),
('TRF', 2221, 1, 1825, 27, 420, 67778, 'Salmo trutta fario', 'Truite'),
('PHX', 2124, 2, 10038, 8, 4949, 196170, 'Phoxinus', 'Vairon'),
('VAN', 2122, 50.3, 2518.3, 18, 442, 67295, 'Leuciscus leuciscus', 'Vandoise');

-- 1) Rattache le code SANDRE aux espèces déjà présentes en base (jeux de
-- référence dev/recette notamment), matchées sans accent sur le nom usuel
-- principal (avant la première virgule), comme le fait déjà ImportDao pour
-- la résolution d'espèce à l'import CSV.
UPDATE public.species s
SET code_espece = t.code_espece,
    code_taxon_sandre = t.code_taxon_sandre,
    code_taxref = t.code_taxref,
    scientific_name = t.nom_scientifique_sandre
FROM species_taxon_staging t
WHERE s.code_espece IS DISTINCT FROM t.code_espece
  AND (
        f_unaccent(lower(s.name)) = f_unaccent(lower(split_part(t.nom_usuel, ',', 1)))
     OR f_unaccent(lower(s.export_as)) = f_unaccent(lower(split_part(t.nom_usuel, ',', 1)))
      );

-- 2) Insère les espèces du référentiel absentes de la base (built_in, taille
-- obligatoire par défaut comme les autres espèces de poisson du seed dev).
INSERT INTO public.species (name, export_as, built_in, mandatory_size, code_espece, code_taxon_sandre, code_taxref, scientific_name)
SELECT split_part(t.nom_usuel, ',', 1), split_part(t.nom_usuel, ',', 1), true, true,
       t.code_espece, t.code_taxon_sandre, t.code_taxref, t.nom_scientifique_sandre
FROM species_taxon_staging t
WHERE NOT EXISTS (SELECT 1 FROM public.species s WHERE s.code_espece = t.code_espece);

-- 3) Bornes de taille (mm -> cm, arrondi vers l'extérieur) et de masse (g,
-- inchangé) pour la détection des valeurs aberrantes (#12 / ImportService).
INSERT INTO public.species_size_bounds (species_id, min_size_cm, max_size_cm, min_weight_g, max_weight_g)
SELECT s.id,
       CASE WHEN t.taille_min_mm IS NULL THEN NULL ELSE floor(t.taille_min_mm / 10.0) END,
       CASE WHEN t.taille_max_mm IS NULL THEN NULL ELSE ceil(t.taille_max_mm / 10.0) END,
       CASE WHEN t.masse_min_g IS NULL THEN NULL ELSE floor(t.masse_min_g) END,
       CASE WHEN t.masse_max_g IS NULL THEN NULL ELSE ceil(t.masse_max_g) END
FROM species_taxon_staging t
JOIN public.species s ON s.code_espece = t.code_espece
ON CONFLICT (species_id) DO UPDATE
SET min_size_cm = EXCLUDED.min_size_cm,
    max_size_cm = EXCLUDED.max_size_cm,
    min_weight_g = EXCLUDED.min_weight_g,
    max_weight_g = EXCLUDED.max_weight_g;

COMMIT;
