package fr.inrae.fishola.rest.imports;

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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Saisie manuelle opérateur (#72).
 *
 * Un seul jeu de règles avec l'import : la résolution d'entité hydro, les bornes de
 * taille (Q8, {@link ImportService#sizeOutOfBounds}) et la persistance {@code Trip}+
 * {@code Catch} sont réutilisées telles quelles ({@link ImportDao}).
 */
@Singleton
public class ManualEntryService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Méthodes de recueil ouvertes à l'opérateur (cf. cadrage §3, option a) —
     * {@code saisie_pecheur} est réservé à l'application pêcheur et exclu ici.
     * {@code carnet_volontaire} est exclu depuis #143 : ce mode passe désormais par le
     * pipeline dédié ({@link fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireManualEntryService}).
     */
    public static final Set<String> OPERATOR_COLLECTION_METHODS =
            Set.of("enquete", "carnet_obligatoire");

    @Inject
    protected ImportDao importDao;

    private static String emptyToNull(String v) {
        return (v == null || v.isEmpty()) ? null : v;
    }

    /**
     * Valide (structurel + référentiel + métier) puis, si tout est valide, persiste la
     * saisie. {@code allowedWaterEntities == null} = pas de restriction (admin national).
     */
    public ManualResultBean submit(ManualTripBean bean, Set<UUID> allowedWaterEntities, LocalDate today) {
        List<ManualError> errors = new ArrayList<>();
        if (bean == null) {
            errors.add(new ManualError(null, null, "corps de requête manquant"));
            return new ManualResultBean(null, 0, errors);
        }

        // --- Structurel ---
        String cm = bean.collectionMethod;
        if (cm == null || !OPERATOR_COLLECTION_METHODS.contains(cm)) {
            errors.add(new ManualError(null, "collectionMethod",
                    "méthode de recueil invalide (enquête / carnet volontaire / carnet obligatoire)"));
        }
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

        List<ManualCatchBean> captures = bean.captures == null ? List.of() : bean.captures;
        for (int i = 0; i < captures.size(); i++) {
            ManualCatchBean c = captures.get(i);
            if (c.speciesId == null || !importDao.existsSpecies(c.speciesId)) {
                errors.add(new ManualError(i, "speciesId", "espèce invalide"));
            }
        }

        // --- Métier (règles partagées avec l'import) ---
        errors.addAll(validateBusiness(bean.bredouille, captures));

        if (!errors.isEmpty()) {
            return new ManualResultBean(null, 0, errors);
        }

        // --- Persistance ---
        String name = "Saisie " + cm + " " + bean.day.format(DAY_FMT);
        List<ImportDao.ManualCatch> rows = bean.bredouille ? List.of() : captures.stream()
                .map(c -> new ImportDao.ManualCatch(c.speciesId, c.techniqueId, c.size, c.weight, c.kept,
                        c.quantity, emptyToNull(c.sizeClass), emptyToNull(c.description)))
                .toList();

        UUID tripId = importDao.saveManualEntry(cm, bean.day, bean.startTime, bean.endTime, waterEntityId,
                name, bean.techniqueId, rows);
        return new ManualResultBean(tripId, rows.size(), List.of());
    }

    /** Validation métier (portage de {@code validate_manual}) : bredouille, lot ≥ 1, taille aberrante. */
    List<ManualError> validateBusiness(boolean bredouille, List<ManualCatchBean> captures) {
        List<ManualError> errors = new ArrayList<>();
        if (bredouille) {
            if (!captures.isEmpty()) {
                errors.add(new ManualError(null, "bredouille",
                        "sortie déclarée bredouille mais des captures sont renseignées"));
            }
            return errors; // une sortie bredouille n'a pas de capture à valider
        }
        if (captures.isEmpty()) {
            errors.add(new ManualError(null, null, "ajoutez au moins une capture, ou cochez « Bredouille »"));
            return errors;
        }
        for (int i = 0; i < captures.size(); i++) {
            ManualCatchBean c = captures.get(i);
            if (c.quantity == null || c.quantity < 1) {
                errors.add(new ManualError(i, "quantity", "le nombre d'individus d'un lot doit être ≥ 1"));
            }
            if (c.size != null) {
                ImportDao.Bounds bounds = importDao.sizeBounds(c.speciesId);
                if (ImportService.sizeOutOfBounds(bounds, c.size)) {
                    errors.add(new ManualError(i, "size",
                            "taille " + c.size + " cm hors bornes [" + bounds.min() + "-" + bounds.max()
                                    + "] pour l'espèce"));
                }
            }
        }
        return errors;
    }

    /** Entité hydro : id prioritaire (clic-carte, sans ambiguïté), repli sur la résolution par nom. */
    private UUID resolveWaterEntity(ManualTripBean bean) {
        if (bean.waterEntityId != null && importDao.existsWaterEntity(bean.waterEntityId)) {
            return bean.waterEntityId;
        }
        return importDao.resolveWaterEntity(bean.eauNom, bean.commune);
    }
}
