package fr.inrae.fishola.rest.imports.survey;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import fr.inrae.fishola.entities.enums.DayPeriod;
import fr.inrae.fishola.entities.enums.FishingMode;
import fr.inrae.fishola.rest.imports.ImportDao;
import fr.inrae.fishola.rest.imports.ImportService;
import fr.inrae.fishola.rest.imports.ManualError;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Saisie manuelle opérateur, format « enquête terrain » (#144) — formulaire assistant
 * session -> sortie -> pêcheur(s) -> captures, avec bloc « session souvenir » facultatif
 * par pêcheur. Mêmes principes que {@link
 * fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireManualEntryService} (#143) : un seul
 * jeu de règles avec l'import ({@link SurveyImportService}), réutilisation de la résolution
 * d'entité hydro, des bornes de taille (Q8, {@link ImportService#sizeOutOfBounds}) et de la
 * persistance {@code Trip}+{@code Catch} ({@link ImportDao#saveManualEntrySurvey}).
 */
@Singleton
public class SurveyManualEntryService {

    private static final Map<DayPeriod, String> DAY_PERIOD_LABELS = Map.of(
            DayPeriod.matin, "matin", DayPeriod.apres_midi, "apres midi",
            DayPeriod.journee_entiere, "journee entiere", DayPeriod.soiree, "soiree");

    @Inject
    protected ImportDao importDao;

    private static String normalize(String v) {
        String s = Normalizer.normalize(v.strip().toLowerCase(), Normalizer.Form.NFD);
        return s.replaceAll("\\p{M}", "").replace('-', ' ').replaceAll("\\s+", " ").strip();
    }

    private static DayPeriod resolveDayPeriod(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = normalize(raw);
        for (Map.Entry<DayPeriod, String> e : DAY_PERIOD_LABELS.entrySet()) {
            if (e.getValue().equals(normalized)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static FishingMode resolveFishingMode(String raw) {
        if (raw == null || !SurveySchema.MODES_PECHE.contains(normalize(raw))) {
            return null;
        }
        return switch (normalize(raw)) {
            case "bateau" -> FishingMode.bateau;
            case "float tube/canoe" -> FishingMode.float_tube_canoe;
            case "bord itinerant" -> FishingMode.bord_itinerant;
            case "bord statique" -> FishingMode.bord_statique;
            default -> null;
        };
    }

    public SurveyManualResultBean submit(SurveySortieBean bean, Set<UUID> allowedWaterEntities, LocalDate today) {
        List<ManualError> errors = new ArrayList<>();
        if (bean == null) {
            errors.add(new ManualError(null, null, "corps de requête manquant"));
            return new SurveyManualResultBean(List.of(), 0, errors);
        }

        // --- Sortie : structurel + référentiel ---
        if (bean.day == null) {
            errors.add(new ManualError(null, "day", "date obligatoire"));
        } else if (bean.day.isAfter(today)) {
            errors.add(new ManualError(null, "day", "la date ne peut pas être dans le futur"));
        }
        if (bean.controlTime == null) {
            errors.add(new ManualError(null, "controlTime", "heure du contrôle obligatoire"));
        }
        if (bean.startTime == null) {
            errors.add(new ManualError(null, "startTime", "heure de début obligatoire"));
        }
        if (bean.endTime == null) {
            errors.add(new ManualError(null, "endTime", "heure de fin obligatoire"));
        }
        if (bean.startTime != null && bean.endTime != null && !bean.endTime.isAfter(bean.startTime)) {
            errors.add(new ManualError(null, "endTime", "l'heure de fin doit être postérieure à l'heure de début"));
        }
        if (bean.unsurveyedShoreAnglers != null && bean.unsurveyedShoreAnglers < 0) {
            errors.add(new ManualError(null, "unsurveyedShoreAnglers", "doit être ≥ 0"));
        }
        if (bean.unsurveyedBoatAnglers != null && bean.unsurveyedBoatAnglers < 0) {
            errors.add(new ManualError(null, "unsurveyedBoatAnglers", "doit être ≥ 0"));
        }

        UUID waterEntityId = resolveWaterEntity(bean.waterEntityId, bean.secteur);
        if (waterEntityId == null) {
            errors.add(new ManualError(null, "waterEntityId", "sélectionnez un secteur (carte ou recherche)"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(waterEntityId)) {
            errors.add(new ManualError(null, "waterEntityId", "secteur hors de votre périmètre"));
        }

        List<SurveyAnglerBean> anglers = bean.anglers == null ? List.of() : bean.anglers;
        if (anglers.isEmpty()) {
            errors.add(new ManualError(null, "anglers", "ajoutez au moins un pêcheur interrogé"));
        }

        // --- Pêcheurs : référentiel + métier ---
        List<ImportDao.SurveyManualAngler> resolvedAnglers = new ArrayList<>();
        for (int i = 0; i < anglers.size(); i++) {
            SurveyAnglerBean angler = anglers.get(i);
            resolvedAnglers.add(validateAngler(i, angler, allowedWaterEntities, today, errors));
        }

        if (!errors.isEmpty()) {
            return new SurveyManualResultBean(List.of(), 0, errors);
        }

        ImportDao.ManualSurveyResult result = importDao.saveManualEntrySurvey(waterEntityId, bean.day,
                bean.controlTime, bean.startTime, bean.endTime,
                bean.unsurveyedShoreAnglers == null ? null : bean.unsurveyedShoreAnglers.shortValue(),
                bean.unsurveyedBoatAnglers == null ? null : bean.unsurveyedBoatAnglers.shortValue(),
                resolvedAnglers);

        int totalCatches = resolvedAnglers.stream().mapToInt(a -> a.catches().size()
                + (a.souvenir() != null && a.souvenir().catch_() != null ? 1 : 0)).sum();
        return new SurveyManualResultBean(result.tripIds(), totalCatches, List.of());
    }

    private ImportDao.SurveyManualAngler validateAngler(int index, SurveyAnglerBean angler,
                                                         Set<UUID> allowedWaterEntities, LocalDate today,
                                                         List<ManualError> errors) {
        FishingMode fishingMode = resolveFishingMode(angler.fishingMode);
        if (fishingMode == null) {
            errors.add(new ManualError(index, "fishingMode", "mode de pêche invalide"));
        }
        if (angler.techniqueId == null || !importDao.existsTechnique(angler.techniqueId)) {
            errors.add(new ManualError(index, "techniqueId", "technique invalide"));
        }
        if (angler.rodCount == null || angler.rodCount < 1) {
            errors.add(new ManualError(index, "rodCount", "le nombre de lignes doit être ≥ 1"));
        }
        if (!angler.noExpectedSpecies && angler.expectedSpeciesId == null) {
            errors.add(new ManualError(index, "expectedSpeciesId",
                    "choisissez une espèce recherchée, ou cochez « Aucune »/« Toutes espèces »"));
        } else if (angler.expectedSpeciesId != null && !importDao.existsSpecies(angler.expectedSpeciesId)) {
            errors.add(new ManualError(index, "expectedSpeciesId", "espèce recherchée invalide"));
        }

        List<SurveyCatchBean> captures = angler.captures == null ? List.of() : angler.captures;
        List<ImportDao.SurveyManualCatch> resolvedCatches = validateCatches(index, "", angler.bredouille, captures, errors);

        ImportDao.SurveyManualSouvenir souvenir = null;
        if (angler.souvenir != null) {
            souvenir = validateSouvenir(index, angler.souvenir, allowedWaterEntities, today, errors);
        }

        SurveyAnglerOrigin origin = SurveyAnglerOrigin.resolve(angler.origin);
        return new ImportDao.SurveyManualAngler(origin, fishingMode, angler.techniqueId,
                angler.rodCount == null ? null : angler.rodCount.shortValue(), angler.baitOrLure,
                angler.noExpectedSpecies ? null : angler.expectedSpeciesId, resolvedCatches, souvenir);
    }

    private ImportDao.SurveyManualSouvenir validateSouvenir(int index, SurveySouvenirBean souvenir,
                                                             Set<UUID> allowedWaterEntities, LocalDate today,
                                                             List<ManualError> errors) {
        if (souvenir.day == null) {
            errors.add(new ManualError(index, "souvenir.day", "date obligatoire"));
        } else if (souvenir.day.isAfter(today)) {
            errors.add(new ManualError(index, "souvenir.day", "la date ne peut pas être dans le futur"));
        }
        DayPeriod dayPeriod = resolveDayPeriod(souvenir.dayPeriod);
        if (dayPeriod == null) {
            errors.add(new ManualError(index, "souvenir.dayPeriod", "période de la journée invalide"));
        }
        UUID waterEntityId = resolveWaterEntity(souvenir.waterEntityId, souvenir.sitePeche);
        if (waterEntityId == null) {
            errors.add(new ManualError(index, "souvenir.waterEntityId", "sélectionnez un site pêché"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(waterEntityId)) {
            errors.add(new ManualError(index, "souvenir.waterEntityId", "site pêché hors de votre périmètre"));
        }

        FishingMode fishingMode = resolveFishingMode(souvenir.fishingMode);
        if (fishingMode == null) {
            errors.add(new ManualError(index, "souvenir.fishingMode", "mode de pêche invalide"));
        }
        if (souvenir.techniqueId == null || !importDao.existsTechnique(souvenir.techniqueId)) {
            errors.add(new ManualError(index, "souvenir.techniqueId", "technique invalide"));
        }
        if (souvenir.rodCount == null || souvenir.rodCount < 1) {
            errors.add(new ManualError(index, "souvenir.rodCount", "le nombre de lignes doit être ≥ 1"));
        }
        if (!souvenir.noExpectedSpecies && souvenir.expectedSpeciesId == null) {
            errors.add(new ManualError(index, "souvenir.expectedSpeciesId",
                    "choisissez une espèce recherchée, ou cochez « Aucune »/« Toutes espèces »"));
        } else if (souvenir.expectedSpeciesId != null && !importDao.existsSpecies(souvenir.expectedSpeciesId)) {
            errors.add(new ManualError(index, "souvenir.expectedSpeciesId", "espèce recherchée invalide"));
        }

        ImportDao.SurveyManualCatch resolvedCatch = null;
        if (souvenir.bredouille) {
            if (souvenir.capture != null) {
                errors.add(new ManualError(index, "souvenir.bredouille",
                        "déclaré bredouille mais une capture est renseignée"));
            }
        } else if (souvenir.capture == null) {
            errors.add(new ManualError(index, "souvenir.capture", "ajoutez une capture, ou cochez « Bredouille »"));
        } else {
            resolvedCatch = validateCatch(index, "souvenir.capture", souvenir.capture, errors);
        }

        return new ImportDao.SurveyManualSouvenir(souvenir.day, dayPeriod, waterEntityId, fishingMode,
                souvenir.techniqueId, souvenir.rodCount == null ? null : souvenir.rodCount.shortValue(),
                souvenir.baitOrLure, souvenir.noExpectedSpecies ? null : souvenir.expectedSpeciesId, resolvedCatch);
    }

    /** Validation métier partagée avec l'import (Q8, lots, bredouille). */
    private List<ImportDao.SurveyManualCatch> validateCatches(int anglerIndex, String fieldPrefix, boolean bredouille,
                                                               List<SurveyCatchBean> captures,
                                                               List<ManualError> errors) {
        if (bredouille) {
            if (!captures.isEmpty()) {
                errors.add(new ManualError(anglerIndex, fieldPrefix + "bredouille",
                        "déclaré bredouille mais des captures sont renseignées"));
            }
            return List.of();
        }
        if (captures.isEmpty()) {
            errors.add(new ManualError(anglerIndex, fieldPrefix + "captures",
                    "ajoutez au moins une capture, ou cochez « Bredouille »"));
            return List.of();
        }
        List<ImportDao.SurveyManualCatch> resolved = new ArrayList<>();
        for (int i = 0; i < captures.size(); i++) {
            resolved.add(validateCatch(anglerIndex, fieldPrefix + "captures[" + i + "]", captures.get(i), errors));
        }
        return resolved;
    }

    private ImportDao.SurveyManualCatch validateCatch(int anglerIndex, String field, SurveyCatchBean c,
                                                       List<ManualError> errors) {
        if (c.speciesId == null || !importDao.existsSpecies(c.speciesId)) {
            errors.add(new ManualError(anglerIndex, field + ".speciesId", "espèce invalide"));
            return new ImportDao.SurveyManualCatch(c.speciesId, null, null, c.quantity, c.kept, null, null);
        }
        if (c.quantity == null || c.quantity < 1) {
            errors.add(new ManualError(anglerIndex, field + ".quantity", "le nombre d'individus d'un lot doit être ≥ 1"));
            return new ImportDao.SurveyManualCatch(c.speciesId, null, null, c.quantity, c.kept, null, null);
        }

        boolean isLot = c.quantity > 1;
        Integer size = null;
        Short lotMinSize = null;
        Short lotMaxSize = null;
        if (isLot) {
            if (c.lotMinSize == null || c.lotMaxSize == null) {
                errors.add(new ManualError(anglerIndex, field + ".lotMinSize",
                        "taille min et taille max obligatoires pour un lot (nombre > 1)"));
            } else if (c.lotMinSize > c.lotMaxSize) {
                errors.add(new ManualError(anglerIndex, field + ".lotMinSize", "la taille min doit être ≤ à la taille max"));
            } else {
                lotMinSize = c.lotMinSize.shortValue();
                lotMaxSize = c.lotMaxSize.shortValue();
            }
        } else {
            if (c.size == null && importDao.isSizeMandatory(c.speciesId)) {
                errors.add(new ManualError(anglerIndex, field + ".size", "taille obligatoire pour cette espèce"));
            }
            if (c.size != null) {
                ImportDao.Bounds bounds = importDao.sizeBounds(c.speciesId);
                if (ImportService.sizeOutOfBounds(bounds, c.size)) {
                    errors.add(new ManualError(anglerIndex, field + ".size",
                            "taille " + c.size + " cm hors bornes [" + bounds.min() + "-" + bounds.max()
                                    + "] pour l'espèce"));
                }
                size = c.size;
            }
        }
        return new ImportDao.SurveyManualCatch(c.speciesId, null, size, c.quantity, c.kept, lotMinSize, lotMaxSize);
    }

    private UUID resolveWaterEntity(UUID id, String name) {
        if (id != null && importDao.existsWaterEntity(id)) {
            return id;
        }
        return importDao.resolveWaterEntity(name, null);
    }
}
