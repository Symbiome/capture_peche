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

import fr.inrae.fishola.database.HydroSearchDao;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Point saisi sur la carte par le staff dans une saisie manuelle (enquête, carnet
 * volontaire) : validation, contrôle de périmètre et rattachement à l'entité choisie,
 * comme pour la position d'une sortie saisie par le pêcheur (#9).
 */
@Singleton
public class ManualPositionService {

    @Inject
    protected HydroSearchDao hydroSearchDao;

    /**
     * Valide un point facultatif. Latitude et longitude vont ensemble ; hors national, le
     * point doit tomber dans l'un des départements du périmètre : la sortie est rattachée
     * au département de son point (arbitrage A5) et l'opérateur ne doit pas créer de données
     * hors périmètre (arbitrage A7).
     *
     * @param allowedDepartments périmètre de l'auteur ; vide = national, sans restriction
     * @return la position à enregistrer, vide si aucun point n'est saisi ou en cas d'erreur
     */
    public Optional<ImportDao.ManualPosition> resolve(Double lat, Double lng, UUID waterEntityId,
                                                      Set<String> allowedDepartments, Integer index,
                                                      String field, List<ManualError> errors) {
        if (lat == null && lng == null) {
            return Optional.empty();
        }
        if (lat == null || lng == null) {
            errors.add(new ManualError(index, field, "position incomplète : latitude et longitude sont requises"));
            return Optional.empty();
        }
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            errors.add(new ManualError(index, field, "position invalide"));
            return Optional.empty();
        }
        if (!allowedDepartments.isEmpty()) {
            Optional<String> department = hydroSearchDao.departmentAt(lat, lng);
            if (department.isEmpty() || !allowedDepartments.contains(department.get())) {
                errors.add(new ManualError(index, field, "position hors de votre périmètre"));
                return Optional.empty();
            }
        }
        HydroSearchDao.TripAttribution attribution = waterEntityId == null ? null
                : hydroSearchDao.computeTripAttribution(lat, lng, waterEntityId).orElse(null);
        return Optional.of(new ImportDao.ManualPosition(lat, lng, attribution));
    }
}
