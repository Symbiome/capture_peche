package fr.inrae.fishola.rest.imports.survey;

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

import fr.inrae.fishola.rest.imports.ManualError;

import java.util.List;
import java.util.UUID;

/**
 * Résultat d'une saisie manuelle « enquête terrain » (#144) : une sortie enquêtée peut
 * produire plusieurs {@code Trip} (une par pêcheur interrogé, plus une par session
 * souvenir facultative) — pendant de {@link fr.inrae.fishola.rest.imports.ManualResultBean}
 * (#72) pour ce format multi-sorties.
 */
public class SurveyManualResultBean {

    public List<UUID> tripIds;
    public int captures;
    public List<ManualError> errors;

    public SurveyManualResultBean(List<UUID> tripIds, int captures, List<ManualError> errors) {
        this.tripIds = tripIds;
        this.captures = captures;
        this.errors = errors;
    }
}
