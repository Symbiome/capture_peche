# Correspondance Darwin Core des sorties et captures

Proposition de correspondance entre les colonnes des vues d'export
(`catchs_openadom_export`, `catchs_pending_validation`) et les termes
[Darwin Core](https://dwc.tdwg.org/terms/), à reprendre dans le pipeline ETL
(dbt / Dagster) vers le Data Warehouse. Les colonnes d'origine, de code
session et de codes espèce viennent de l'issue #235 (migration `V3.1.0`).

## En bref

| Colonne d'export | Terme Darwin Core | Niveau |
|---|---|---|
| `id_capture` | `occurrenceID` | Occurrence |
| `id_sortie` | `eventID` | Événement (sortie) |
| `code_session` | `parentEventID` | Événement parent (session) |
| `origine_donnee` | `samplingProtocol` (+ `basisOfRecord`) | Événement |
| `espece_capturee` | `scientificName` (via le référentiel espèces) | Occurrence |
| `code_espece_capturee` | `taxonID` | Occurrence |
| `code_espece_recherchee` | `eco:targetTaxonomicScope` (extension Humboldt) | Événement |
| `nombre_de_poissons` | `individualCount` | Occurrence |
| `date_de_la_sortie`, `date_de_fin_de_la_sortie` | `eventDate` (intervalle ISO 8601) | Événement |

## Origine de la donnée

`origine_donnee` reprend la valeur technique de `trip.collection_method`,
stable pour l'ETL. Toutes les captures sont observées par un humain :
`basisOfRecord = HumanObservation`. L'origine se traduit dans
`samplingProtocol` :

| `origine_donnee` | Libellé back-office | `samplingProtocol` proposé |
|---|---|---|
| `saisie_pecheur` | Application pêcheur | Déclaration du pêcheur dans l'application Fishola |
| `enquete` | Enquête terrain | Enquête de pêche en action (creel survey) |
| `enquete_souvenir` | Enquête – sortie souvenir | Enquête de pêche, déclaration d'une sortie passée |
| `carnet_volontaire` | Carnet volontaire | Carnet de pêche volontaire |
| `carnet_obligatoire` | Carnet obligatoire | Carnet de pêche obligatoire |

## Hiérarchie des événements

- **Session** (`parentEventID = code_session`) : session d'enquête terrain, ou
  `session_ref` du carnet volontaire. Une sortie souvenir porte le code de la
  session où le pêcheur a été enquêté. Vide pour les saisies de l'application.
- **Sortie** (`eventID = id_sortie`) : identifiant technique, sans donnée
  personnelle.
- **Capture** (`occurrenceID = id_capture`).

`code_session` est un code métier, unique dans son origine mais pas
forcément entre origines : préfixer par l'origine dans l'ETL
(ex. `enquete:ENQ-001`) avant de l'utiliser comme `parentEventID`.

## Codes espèce

`code_espece_capturee` et `code_espece_recherchee` sont les codes SANDRE à
3 lettres (`TRF`, `PER`, `BRO`…) de `species.code_espece`, renseignés par le
référentiel espèces UFBRMC. Ils complètent `scientificName` sans le remplacer :
`taxonID` gagne à être préfixé (ex. `sandre:PER`), et le lien vers TAXREF
(`scientificNameID`) reste à faire (cf. user story #7).

`code_espece_recherchee` liste les espèces recherchées séparées par des
virgules (plusieurs possibles dans l'application, une seule en enquête et en
carnet). Elle décrit la sortie, pas la capture : sa place est au niveau de
l'événement, dans l'extension Humboldt (`eco:targetTaxonomicScope`).

## À valider

Cette correspondance est une proposition de Symbiome, à valider avec le
maître d'ouvrage avant l'écriture des modèles dbt : libellés de
`samplingProtocol`, préfixes de `parentEventID` et de `taxonID`, usage de
l'extension Humboldt.
