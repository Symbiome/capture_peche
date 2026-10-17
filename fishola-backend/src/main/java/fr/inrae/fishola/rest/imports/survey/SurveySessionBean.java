package fr.inrae.fishola.rest.imports.survey;

/*-
 * #%L
 * Carnet de pêche :: Backend
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
 * Ouverture d'une session d'enquête saisie manuellement (#236) : premier niveau de la
 * saisie pyramidale session → sortie(s) → pêcheur(s) → captures. Le code session est
 * généré côté serveur ; heure de fin et compteurs de pêcheurs non enquêtés sont saisis à
 * la clôture ({@link SurveySessionCloseBean}).
 */
public class SurveySessionBean {

    public LocalDate day;
    public LocalTime startTime;

    // Localisation : id prioritaire, repli par nom.
    public UUID waterEntityId;
    public String secteur;

    /** Enquêteur, pré-rempli avec le compte connecté côté front mais modifiable. */
    public String operatorName;
}
