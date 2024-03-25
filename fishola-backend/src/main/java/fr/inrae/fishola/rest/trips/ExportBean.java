package fr.inrae.fishola.rest.trips;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2024 INRAE - UMR CARRTEL
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

public class ExportBean {
    public String nomDeLaPlateforme;
    public String dateDeLaSortie;
    public String typeDePeche;
    public String especeCapturee;
    public Integer longueurTotaleDuPoisson;
    public Integer longueurTotaleDuPoissonCalculee;
    public Integer poidsDuPoisson;
    public String aExclure;

    public UUID catchId;
}
