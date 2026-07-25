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
import java.util.UUID;

/**
 * Valeurs d'une ligne CSV validée et coercée, prêtes pour la persistance.
 * Les champs de sortie (collectionMethod, day, start, end, waterEntityId, techniqueId)
 * sont portés par la première ligne d'une {@code session_ref} ; les champs de capture
 * (species, quantity, longueur, weight, kept, sizeClass, description) sont propres à la ligne.
 */
public class ParsedRow {

    public int line;

    // Niveau sortie (trip)
    public String collectionMethod;
    public LocalDate day;
    public LocalTime start;
    public LocalTime end;
    public boolean bredouille;
    public UUID waterEntityId;
    public UUID techniqueId;

    // Niveau capture (catch)
    public boolean hasCapture;
    public UUID speciesId;
    public Integer quantity;
    public Integer longueur;
    public Integer weight;
    public boolean kept;
    public String sizeClass;
    public String description;
}
