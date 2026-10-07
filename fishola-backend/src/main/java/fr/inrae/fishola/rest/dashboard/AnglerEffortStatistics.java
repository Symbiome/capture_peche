package fr.inrae.fishola.rest.dashboard;

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.inrae.fishola.ImmutableObject;

import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Statistiques d'effort de pêche d'un pêcheur (#210), filtrées comme le
 * tableau de bord personnel (année, milieu) : sessions et heures par mois,
 * temps par technique, CPUE par espèce et part de contribution au secteur.
 * Les prises comptent le nombre de poissons des lots (#148).
 */
@ImmutableObject
@JsonSerialize(as = ImmutableAnglerEffortStatistics.class)
public interface AnglerEffortStatistics {

    /** Nombre de sessions et heures de pêche par mois (mois sans session absents). */
    Map<Month, MonthlyEffort> monthlyEffort();

    /** Heures de pêche cumulées sur la période. */
    double totalHours();

    /**
     * Heures par technique : la durée d'une session est répartie à parts égales
     * entre ses techniques ; sessions sans technique non comptées.
     */
    Map<UUID, Double> hoursPerTechnique();

    /** Nombre minimal de prises d'une espèce pour afficher sa CPUE. */
    int cpueMinCatches();

    /** Prises par espèce, avec leur CPUE au-delà du seuil. */
    List<SpeciesCpue> cpuePerSpecies();

    /** Nombre minimal de pêcheurs sur le secteur pour afficher la contribution. */
    int sectorMinAnglers();

    /**
     * Part du pêcheur dans le secteur (milieu filtré) : agrégats seulement,
     * vide sans filtre de milieu ou sous le seuil de pêcheurs.
     */
    Optional<SectorContribution> sectorContribution();

    @ImmutableObject
    @JsonSerialize(as = ImmutableMonthlyEffort.class)
    interface MonthlyEffort {

        int tripsCount();

        double hours();
    }

    @ImmutableObject
    @JsonSerialize(as = ImmutableSpeciesCpue.class)
    interface SpeciesCpue {

        UUID speciesId();

        int catchesCount();

        /** Prises par heure de pêche ; vide sous le seuil de prises. */
        Optional<Double> catchesPerHour();
    }

    @ImmutableObject
    @JsonSerialize(as = ImmutableSectorContribution.class)
    interface SectorContribution {

        int anglersCount();

        int myTripsCount();

        int totalTripsCount();

        int myCatchesCount();

        int totalCatchesCount();
    }
}
