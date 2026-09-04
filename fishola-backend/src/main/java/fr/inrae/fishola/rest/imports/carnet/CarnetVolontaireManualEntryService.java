package fr.inrae.fishola.rest.imports.carnet;

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

import fr.inrae.fishola.rest.imports.ImportDao;
import fr.inrae.fishola.rest.imports.ImportService;
import fr.inrae.fishola.rest.imports.ManualError;
import fr.inrae.fishola.rest.imports.ManualResultBean;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Saisie manuelle opérateur, format « carnet volontaire » (#143) — dédiée, distincte du
 * formulaire générique ({@link fr.inrae.fishola.rest.imports.ManualEntryService}, #72).
 * Mêmes principes : un seul jeu de règles avec l'import ({@link CarnetVolontaireImportService}),
 * réutilisation de la résolution d'entité hydro, des bornes de taille (Q8,
 * {@link ImportService#sizeOutOfBounds}) et de la persistance {@code Trip}+{@code Catch}
 * ({@link ImportDao#saveManualEntry}).
 */
@Singleton
public class CarnetVolontaireManualEntryService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Inject
    protected ImportDao importDao;

    private static String normalize(String v) {
        String s = Normalizer.normalize(v.strip().toLowerCase(), Normalizer.Form.NFD);
        return s.replaceAll("\\p{M}", "");
    }

    public ManualResultBean submit(CarnetVolontaireTripBean bean, Set<UUID> allowedWaterEntities, LocalDate today) {
        List<ManualError> errors = new ArrayList<>();
        if (bean == null) {
            errors.add(new ManualError(null, null, "corps de requête manquant"));
            return new ManualResultBean(null, 0, errors);
        }

        // --- Structurel ---
        if (bean.day == null) {
            errors.add(new ManualError(null, "day", "date obligatoire"));
        } else if (bean.day.isAfter(today)) {
            errors.add(new ManualError(null, "day", "la date ne peut pas être dans le futur"));
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
        if (bean.fishingMode == null || !CarnetVolontaireSchema.MODES_PECHE.contains(normalize(bean.fishingMode))) {
            errors.add(new ManualError(null, "fishingMode", "mode de pêche invalide"));
        }
        if (bean.rodCount == null || bean.rodCount < 1) {
            errors.add(new ManualError(null, "rodCount", "le nombre de lignes doit être ≥ 1"));
        }

        // --- Référentiel ---
        UUID waterEntityId = resolveWaterEntity(bean);
        if (waterEntityId == null) {
            errors.add(new ManualError(null, "waterEntityId",
                    "sélectionnez une entité hydrographique (carte ou recherche)"));
        } else if (allowedWaterEntities != null && !allowedWaterEntities.contains(waterEntityId)) {
            errors.add(new ManualError(null, "waterEntityId", "entité hydrographique hors de votre périmètre"));
        }

        if (bean.techniqueId == null || !importDao.existsTechnique(bean.techniqueId)) {
            errors.add(new ManualError(null, "techniqueId", "technique invalide"));
        }
        if (bean.secondaryTechniqueId != null && !importDao.existsTechnique(bean.secondaryTechniqueId)) {
            errors.add(new ManualError(null, "secondaryTechniqueId", "technique secondaire invalide"));
        }
        if (!bean.noExpectedSpecies && bean.expectedSpeciesId == null) {
            errors.add(new ManualError(null, "expectedSpeciesId",
                    "choisissez une espèce recherchée, ou cochez « Aucune »"));
        } else if (bean.expectedSpeciesId != null && !importDao.existsSpecies(bean.expectedSpeciesId)) {
            errors.add(new ManualError(null, "expectedSpeciesId", "espèce recherchée invalide"));
        }

        List<CarnetVolontaireCatchBean> captures = bean.captures == null ? List.of() : bean.captures;
        for (int i = 0; i < captures.size(); i++) {
            CarnetVolontaireCatchBean c = captures.get(i);
            if (c.speciesId == null || !importDao.existsSpecies(c.speciesId)) {
                errors.add(new ManualError(i, "speciesId", "espèce invalide"));
            }
            if (c.techniqueId != null && !importDao.existsTechnique(c.techniqueId)) {
                errors.add(new ManualError(i, "techniqueId", "technique de capture invalide"));
            }
            if (c.troutOrigin != null && !CarnetVolontaireSchema.TROUT_ORIGINS.contains(normalize(c.troutOrigin))) {
                errors.add(new ManualError(i, "troutOrigin", "origine TRF invalide"));
            }
        }

        // --- Métier (règles partagées avec l'import) ---
        errors.addAll(validateBusiness(bean.bredouille, captures));

        if (!errors.isEmpty()) {
            return new ManualResultBean(null, 0, errors);
        }

        // --- Persistance ---
        String name = "Carnet volontaire " + bean.day.format(DAY_FMT);
        List<ImportDao.ManualCatch> rows = bean.bredouille ? List.of() : captures.stream()
                .map(c -> {
                    boolean isLot = c.quantity != null && c.quantity > 1;
                    String sizeClass = isLot && c.lotMinSize != null && c.lotMaxSize != null
                            ? (c.lotMinSize + "-" + c.lotMaxSize) : null;
                    Integer size = isLot ? null : c.size;
                    UUID technique = c.techniqueId != null ? c.techniqueId : bean.techniqueId;
                    return new ImportDao.ManualCatch(c.speciesId, technique, size, c.weight, c.kept,
                            c.quantity, sizeClass, null);
                })
                .toList();

        UUID tripId = importDao.saveManualEntry("carnet_volontaire", bean.day, bean.startTime, bean.endTime,
                waterEntityId, name, bean.techniqueId, rows);
        return new ManualResultBean(tripId, rows.size(), List.of());
    }

    /** Validation métier : bredouille, lot ≥ 1, bornes de lot, taille aberrante, marquage. */
    List<ManualError> validateBusiness(boolean bredouille, List<CarnetVolontaireCatchBean> captures) {
        List<ManualError> errors = new ArrayList<>();
        if (bredouille) {
            if (!captures.isEmpty()) {
                errors.add(new ManualError(null, "bredouille",
                        "sortie déclarée bredouille mais des captures sont renseignées"));
            }
            return errors;
        }
        if (captures.isEmpty()) {
            errors.add(new ManualError(null, null, "ajoutez au moins une capture, ou cochez « Bredouille »"));
            return errors;
        }
        for (int i = 0; i < captures.size(); i++) {
            CarnetVolontaireCatchBean c = captures.get(i);
            if (c.quantity == null || c.quantity < 1) {
                errors.add(new ManualError(i, "quantity", "le nombre d'individus d'un lot doit être ≥ 1"));
                continue;
            }
            boolean isLot = c.quantity > 1;
            if (isLot) {
                if (c.lotMinSize == null || c.lotMaxSize == null) {
                    errors.add(new ManualError(i, "lotMinSize",
                            "taille min et taille max obligatoires pour un lot (nombre > 1)"));
                } else if (c.lotMinSize > c.lotMaxSize) {
                    errors.add(new ManualError(i, "lotMinSize", "la taille min doit être ≤ à la taille max"));
                }
            } else {
                if (c.size == null && c.speciesId != null && importDao.isSizeMandatory(c.speciesId)) {
                    errors.add(new ManualError(i, "size", "taille obligatoire pour cette espèce"));
                }
                if (c.size != null && c.speciesId != null) {
                    ImportDao.Bounds bounds = importDao.sizeBounds(c.speciesId);
                    if (ImportService.sizeOutOfBounds(bounds, c.size)) {
                        errors.add(new ManualError(i, "size",
                                "taille " + c.size + " cm hors bornes [" + bounds.min() + "-" + bounds.max()
                                        + "] pour l'espèce"));
                    }
                }
            }
            if (c.tagged && (c.tagReference == null || c.tagReference.isBlank())) {
                errors.add(new ManualError(i, "tagReference",
                        "numéro de marquage obligatoire quand le poisson est marqué/bagué"));
            }
        }
        return errors;
    }

    private UUID resolveWaterEntity(CarnetVolontaireTripBean bean) {
        if (bean.waterEntityId != null && importDao.existsWaterEntity(bean.waterEntityId)) {
            return bean.waterEntityId;
        }
        return importDao.resolveWaterEntity(bean.eauNom, bean.commune);
    }
}
