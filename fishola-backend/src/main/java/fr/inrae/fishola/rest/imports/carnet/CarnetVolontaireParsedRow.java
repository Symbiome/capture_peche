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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Valeurs d'une ligne CSV « carnet volontaire » validée et coercée, prêtes pour la
 * persistance. Les champs de sortie sont portés par la première ligne d'un
 * {@code session_ref} ; les champs de capture sont propres à la ligne.
 *
 * <p>Les champs marqués {@code TODO(#145)} sont validés mais non persistés : ils
 * n'ont pas encore de colonne dédiée (cf. issue #145, extensions de schéma).
 */
public class CarnetVolontaireParsedRow {

    public int line;

    // Niveau sortie (trip)
    public LocalDate day;
    public LocalTime start;
    public LocalTime end;
    public boolean bredouille;
    public UUID waterEntityId;
    public UUID techniqueId;
    public UUID secondaryTechniqueId; // TODO(#145) : validé, non persisté
    public String baitOrLure; // TODO(#145) : validé, non persisté
    public UUID expectedSpeciesId; // TODO(#145) : validé, non persisté (null si "Aucune")
    public String observations; // TODO(#145) : validé, non persisté

    // Niveau capture (catch)
    public boolean hasCapture;
    public UUID speciesId;
    public String troutOrigin; // TODO(#145) : validé, non persisté
    public UUID captureTechniqueId;
    public String captureBaitOrLure; // TODO(#145) : validé, non persisté
    public LocalTime captureTime; // TODO(#145) : validé, non persisté
    public Integer size;
    public Integer weight;
    public boolean kept;
    public Integer quantity;
    public Integer lotMinSize;
    public Integer lotMaxSize;
    public boolean tagged; // TODO(#145) : validé, non persisté
    public String tagReference; // TODO(#145) : validé, non persisté
}
