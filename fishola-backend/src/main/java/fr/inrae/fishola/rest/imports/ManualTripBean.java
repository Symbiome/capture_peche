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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Saisie manuelle d'une sortie opérateur + ses captures (#72). La localisation est
 * posée par identifiant d'entité hydro (clic carte / recherche) ; à défaut, un nom
 * ({@code eauNom} / {@code commune}) est résolu côté serveur comme pour l'import.
 */
public class ManualTripBean {

    public String collectionMethod;
    public LocalDate day;
    public LocalTime startTime;
    public LocalTime endTime;

    // Localisation : id prioritaire, repli par nom.
    public UUID waterEntityId;
    public String eauNom;
    public String commune;

    public UUID techniqueId;

    // Saisis et validés mais non persistés en V1 (mêmes limites que l'import, cf. #65).
    public String fishingMode;
    public UUID targetSpeciesId;

    public boolean bredouille;
    public List<ManualCatchBean> captures;
}
