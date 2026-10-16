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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Contrôle de la fin d'une sortie saisie à la main (#237), commun à la saisie générique,
 * au carnet volontaire et à l'enquête terrain.
 */
public final class ManualTripEndValidation {

    static final String END_TIME_NOT_AFTER_START = "l'heure de fin doit être postérieure à l'heure de début";
    static final String END_NOT_AFTER_START = "la fin de la sortie doit être postérieure à son début";
    static final String END_DAY_IN_FUTURE = "la date de fin ne peut pas être dans le futur";

    private ManualTripEndValidation() {}

    /**
     * Ajoute aux erreurs les incohérences de fin de sortie. Sans date de fin, l'heure de fin
     * doit suivre l'heure de début le même jour (règle antérieure à #237) ; avec une date de
     * fin, la fin doit suivre le début, sur plusieurs jours si besoin.
     *
     * @param day date de début
     * @param startTime heure de début
     * @param endDay date de fin, nulle pour une sortie d'une journée
     * @param endTime heure de fin
     * @param latestEndDay date de fin maximale autorisée, nulle pour une fin prévue (enquête)
     * @param errors erreurs de la saisie, complétées
     */
    public static void validate(LocalDate day, LocalTime startTime, LocalDate endDay, LocalTime endTime,
                                LocalDate latestEndDay, List<ManualError> errors) {
        if (endDay != null && latestEndDay != null && endDay.isAfter(latestEndDay)) {
            errors.add(new ManualError(null, "endDay", END_DAY_IN_FUTURE));
        }
        if (startTime == null || endTime == null) {
            return;
        }
        if (endDay == null) {
            if (!endTime.isAfter(startTime)) {
                errors.add(new ManualError(null, "endTime", END_TIME_NOT_AFTER_START));
            }
            return;
        }
        if (day != null && !LocalDateTime.of(endDay, endTime).isAfter(LocalDateTime.of(day, startTime))) {
            errors.add(new ManualError(null, "endDay", END_NOT_AFTER_START));
        }
    }
}
