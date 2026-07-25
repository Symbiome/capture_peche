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

import java.util.UUID;

/** Une capture (ou un lot) saisie manuellement. L'espèce est choisie par identifiant. */
public class ManualCatchBean {

    public UUID speciesId;
    public Integer quantity;
    public Integer size;
    public Integer weight;
    public String sizeClass;
    public boolean kept;
    public String description;
    /** Technique propre à la capture ; à défaut, celle de la sortie s'applique. */
    public UUID techniqueId;
}
