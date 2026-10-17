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

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Session d'enquête telle que l'affiche la saisie pyramidale (#236) : rappel de la
 * session en cours, liste des sessions ouvertes à reprendre.
 */
public class SurveySessionView {

    public UUID id;
    public String code;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate day;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    public LocalTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    public LocalTime endTime;
    public UUID waterEntityId;
    public String waterEntityName;
    public String operatorName;
    /** {@code ouverte} ou {@code cloturee}. */
    public String status;
    public Short unsurveyedShoreAnglers;
    public Short unsurveyedBoatAnglers;
    /** Compte staff qui a ouvert la session : seul à pouvoir la compléter et la clôturer. */
    public UUID createdBy;
    public int sortiesCount;
    public int anglersCount;
}
