package fr.inrae.fishola.rest.trips;

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
import java.util.Optional;

/**
 * Règles de dates des sorties (#237), partagées par l'API pêcheur, la saisie manuelle
 * opérateur et les imports : une sortie peut durer plusieurs jours (pêche de la carpe).
 */
public final class TripTimestamps {

    public static final String END_NOT_AFTER_BEGIN = "La fin de la sortie doit être postérieure à son début";
    public static final String DATE_IN_FUTURE = "La date de début ou de fin de la sortie ne peut pas être dans le futur";
    public static final String CATCH_OUT_OF_TRIP =
            "La date et l'heure de la capture doivent être comprises entre le début et la fin de la sortie";

    private TripTimestamps() {}

    /**
     * Horodatage de fin d'une sortie.
     *
     * @param beginDay jour de début
     * @param beginTime heure de début
     * @param endDay jour de fin ; {@code null} pour les clients et formats antérieurs à #237,
     *               qui ne transmettent qu'une date : on applique alors la règle historique
     *               « heure de fin antérieure à l'heure de début → lendemain »
     * @param endTime heure de fin
     * @return la date et l'heure de fin de la sortie
     */
    public static LocalDateTime endTimestamp(LocalDate beginDay, LocalTime beginTime, LocalDate endDay,
                                             LocalTime endTime) {
        if (endDay != null) {
            return LocalDateTime.of(endDay, endTime);
        }
        LocalDateTime sameDay = LocalDateTime.of(beginDay, endTime);
        return sameDay.isBefore(LocalDateTime.of(beginDay, beginTime)) ? sameDay.plusDays(1) : sameDay;
    }

    /**
     * Indique si un instant tombe dans la sortie, bornes incluses.
     *
     * @param instant instant à tester
     * @param begin début de la sortie
     * @param end fin de la sortie
     * @return {@code true} si {@code begin <= instant <= end}
     */
    public static boolean isWithin(LocalDateTime instant, LocalDateTime begin, LocalDateTime end) {
        return !instant.isBefore(begin) && !instant.isAfter(end);
    }

    /**
     * Horodatage d'une capture, qui doit tomber dans la sortie.
     *
     * @param catchDay jour de la capture ; {@code null} pour les clients antérieurs à #237, qui
     *                 n'envoient que l'heure : on retient alors le premier jour de la sortie où
     *                 cette heure tombe dans la sortie (capture après minuit sur une sortie à
     *                 cheval sur minuit, comme le backfill de V1.5.0)
     * @param catchTime heure de la capture
     * @param begin début de la sortie
     * @param end fin de la sortie
     * @return l'horodatage de la capture, vide si elle ne tombe pas dans la sortie
     */
    public static Optional<LocalDateTime> catchTimestamp(LocalDate catchDay, LocalTime catchTime,
                                                         LocalDateTime begin, LocalDateTime end) {
        if (catchDay != null) {
            LocalDateTime candidate = LocalDateTime.of(catchDay, catchTime);
            return isWithin(candidate, begin, end) ? Optional.of(candidate) : Optional.empty();
        }
        for (LocalDate day = begin.toLocalDate(); !day.isAfter(end.toLocalDate()); day = day.plusDays(1)) {
            LocalDateTime candidate = LocalDateTime.of(day, catchTime);
            if (isWithin(candidate, begin, end)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
