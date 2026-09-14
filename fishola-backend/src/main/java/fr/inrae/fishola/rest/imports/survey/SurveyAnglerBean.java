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

import java.util.List;
import java.util.UUID;

/**
 * Un pêcheur interrogé lors d'une sortie enquêtée (#144), saisi manuellement. Non
 * nominatif : seule une origine (département FR ou pays) est conservée. {@code Code
 * pêcheur} est généré côté serveur, jamais saisi (cf. issue #144).
 */
public class SurveyAnglerBean {

    /** Département FR (nom ou code) ou pays si non-résident ; les deux sont facultatifs. */
    public String origin;

    public String fishingMode;
    public UUID techniqueId;
    public Integer rodCount;
    public String baitOrLure;

    /** Espèce recherchée ; ignoré si {@code noExpectedSpecies}. */
    public UUID expectedSpeciesId;
    public boolean noExpectedSpecies;

    public boolean bredouille;
    public List<SurveyCatchBean> captures;

    /** Bloc facultatif : dernière sortie passée du même pêcheur. */
    public SurveySouvenirBean souvenir;
}
