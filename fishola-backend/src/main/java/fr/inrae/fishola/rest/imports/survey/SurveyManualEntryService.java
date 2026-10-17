package fr.inrae.fishola.rest.imports.survey;

/*-
 * #%L
 * Carnet de pêche :: Backend
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
import fr.inrae.fishola.rest.imports.ManualPositionService;
import fr.inrae.fishola.rest.imports.ManualTripEndValidation;
import fr.inrae.fishola.rest.trips.TripTimestamps;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Saisie manuelle opérateur, format « enquête terrain » (#144) — ajout d'une sortie à une
 * session d'enquête ouverte (#236) : pêcheur(s) interrogé(s) -> captures, avec bloc
 * « session souvenir » facultatif par pêcheur. Mêmes principes que {@link
 * fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireManualEntryService} (#143) : un seul
 * jeu de règles avec l'import ({@link SurveyImportService}), réutilisation de la résolution
 * d'entité hydro, des bornes de taille (Q8, {@link ImportService#sizeOutOfBounds}) et de la
 * persistance {@code Trip}+{@code Catch} ({@link ImportDao#saveSurveySortie}).
 */
@Singleton
public class SurveyManualEntryService {

    static final String CONTROL_BEFORE_START = "l'heure du contrôle ne peut pas être antérieure à l'heure de début de pêche";
    static final String CONTROL_AFTER_END = "l'heure du contrôle ne peut pas être postérieure à la fin de pêche prévue";

    private static final Map<DayPeriod, String> DAY_PERIOD_LABELS = Map.of(
            DayPeriod.matin, "matin", DayPeriod.apres_midi, "apres midi",
            DayPeriod.journee_entiere, "journee entiere", DayPeriod.soiree, "soiree");

    @Inject
    protected ImportDao importDao;

    @Inject
    protected ManualPositionService manualPositionService;

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

    /**
     * Valide puis persiste une sortie enquêtée de la session : rien n'est enregistré en cas
     * d'erreur. Date et secteur absents de la sortie reprennent ceux de la session.
     *
     * @param session session d'enquête ouverte
     * @param bean sortie saisie
     * @param allowedWaterEntities secteurs du périmètre, null pour un national
     * @param allowedDepartments départements du périmètre, vide pour un national
     * @param today date du jour (refus des dates futures)
     * @return trips créées et nombre de captures, ou erreurs
     */
    public SurveyManualResultBean submit(SurveySessionView session, SurveySortieBean bean,
                                         Set<UUID> allowedWaterEntities, Set<String> allowedDepartments,
                                         LocalDate today) {
        List<ManualError> errors = new ArrayList<>();
        if (bean == null) {
            errors.add(new ManualError(null, null, "corps de requête manquant"));
            return new SurveyManualResultBean(List.of(), 0, errors);
        }
        LocalDate day = bean.day != null ? bean.day : session.day;
        validateSortieTimes(day, bean, today, errors);

        UUID waterEntityId = bean.waterEntityId == null && bean.secteur == null
                ? session.waterEntityId : resolveWaterEntity(bean.waterEntityId, bean.secteur);
        validateWaterEntity(waterEntityId, allowedWaterEntities, null, "waterEntityId", "secteur", errors);
        ImportDao.ManualPosition position = manualPositionService.resolve(bean.latitude, bean.longitude,
                waterEntityId, allowedDepartments, null, "position", errors).orElse(null);

        List<SurveyAnglerBean> anglers = bean.anglers == null ? List.of() : bean.anglers;
        if (anglers.isEmpty()) {
            errors.add(new ManualError(null, "anglers", "ajoutez au moins un pêcheur interrogé"));
        }
        List<ImportDao.SurveyManualAngler> resolvedAnglers = new ArrayList<>();
        for (int i = 0; i < anglers.size(); i++) {
            resolvedAnglers.add(validateAngler(i, anglers.get(i), allowedWaterEntities, allowedDepartments, today,
                    errors));
        }
        if (!errors.isEmpty()) {
            return new SurveyManualResultBean(List.of(), 0, errors);
        }

        ImportDao.ManualSurveyResult result = importDao.saveSurveySortie(
                new ImportDao.SurveySessionRef(session.id, session.code),
                new ImportDao.SurveyManualSortie(waterEntityId, day, bean.startTime, bean.endDay, bean.endTime,
                        position),
                resolvedAnglers);
        int totalCatches = resolvedAnglers.stream().mapToInt(a -> a.catches().size()
                + (a.souvenir() == null ? 0 : a.souvenir().catches().size())).sum();
        return new SurveyManualResultBean(result.tripIds(), totalCatches, List.of());
    }

    /** Date et heures de la sortie ; heure du contrôle comprise entre le début et la fin prévue (#236). */
    private void validateSortieTimes(LocalDate day, SurveySortieBean bean, LocalDate today, List<ManualError> errors) {
        if (day == null) {
            errors.add(new ManualError(null, "day", "date obligatoire"));
        } else if (day.isAfter(today)) {
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
        int errorsBefore = errors.size();
        ManualTripEndValidation.validate(day, bean.startTime, bean.endDay, bean.endTime, null, errors);
        if (day == null || bean.controlTime == null || bean.startTime == null || bean.endTime == null
                || errors.size() > errorsBefore) {
            return;
        }
        LocalDateTime control = LocalDateTime.of(day, bean.controlTime);
        if (control.isBefore(LocalDateTime.of(day, bean.startTime))) {
            errors.add(new ManualError(null, "controlTime", CONTROL_BEFORE_START));
        } else if (control.isAfter(TripTimestamps.endTimestamp(day, bean.startTime, bean.endDay, bean.endTime))) {
            errors.add(new ManualError(null, "controlTime", CONTROL_AFTER_END));
        }
    }

    /** Secteur obligatoire et, hors national, dans le périmètre de l'opérateur. */
    void validateWaterEntity(UUID waterEntityId, Set<UUID> allowedWaterEntities, Integer index, String field,
                             String label, List<ManualError> errors) {
        if (waterEntityId == null) {
            errors.add(new ManualError(index, field, "sélectionnez un " + label + " (carte ou recherche)"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(waterEntityId)) {
            errors.add(new ManualError(index, field, label + " hors de votre périmètre"));
        }
    }

    private ImportDao.SurveyManualAngler validateAngler(int index, SurveyAnglerBean angler,
                                                         Set<UUID> allowedWaterEntities,
                                                         Set<String> allowedDepartments, LocalDate today,
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
        if (angler.expectedSpeciesId != null && !importDao.existsSpecies(angler.expectedSpeciesId)) {
            errors.add(new ManualError(index, "expectedSpeciesId", "espèce recherchée invalide"));
        }

        List<SurveyCatchBean> captures = angler.captures == null ? List.of() : angler.captures;
        List<ImportDao.SurveyManualCatch> resolvedCatches = validateCatches(index, "", angler.bredouille, captures, errors);

        ImportDao.SurveyManualSouvenir souvenir = null;
        if (angler.souvenir != null) {
            souvenir = validateSouvenir(index, angler.souvenir, allowedWaterEntities, allowedDepartments, today,
                    errors);
        }

        SurveyAnglerOrigin origin = SurveyAnglerOrigin.resolve(angler.origin);
        return new ImportDao.SurveyManualAngler(origin, fishingMode, angler.techniqueId,
                angler.rodCount == null ? null : angler.rodCount.shortValue(), angler.baitOrLure,
                angler.expectedSpeciesId, resolvedCatches, souvenir);
    }

    private ImportDao.SurveyManualSouvenir validateSouvenir(int index, SurveySouvenirBean souvenir,
                                                             Set<UUID> allowedWaterEntities,
                                                             Set<String> allowedDepartments, LocalDate today,
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
        validateWaterEntity(waterEntityId, allowedWaterEntities, index, "souvenir.waterEntityId", "site pêché", errors);
        ImportDao.ManualPosition position = manualPositionService.resolve(souvenir.latitude, souvenir.longitude,
                waterEntityId, allowedDepartments, index, "souvenir.position", errors).orElse(null);

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
        if (souvenir.expectedSpeciesId != null && !importDao.existsSpecies(souvenir.expectedSpeciesId)) {
            errors.add(new ManualError(index, "souvenir.expectedSpeciesId", "espèce recherchée invalide"));
        }

        List<SurveyCatchBean> captures = souvenir.captures == null ? List.of() : souvenir.captures;
        List<ImportDao.SurveyManualCatch> resolvedCatches = validateCatches(index, "souvenir.", souvenir.bredouille,
                captures, errors);

        return new ImportDao.SurveyManualSouvenir(souvenir.day, dayPeriod, waterEntityId, fishingMode,
                souvenir.techniqueId, souvenir.rodCount == null ? null : souvenir.rodCount.shortValue(),
                souvenir.baitOrLure, souvenir.expectedSpeciesId, resolvedCatches, position);
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
