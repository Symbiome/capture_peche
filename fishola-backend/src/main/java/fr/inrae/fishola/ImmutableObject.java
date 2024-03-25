package fr.inrae.fishola;

/*
 * #%L
 * SISPEA Utils
 * %%
 * Copyright (C) 2014 - 2023 OFB
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

import org.immutables.value.Value;

/**
 * Cette annotation permet de définir le "style" commun à toutes les classes immuables du projet.
 */
@Value.Style(
        jakarta = true,
        // Le builder aura une méthode "fromInstance" qui permet de l'initialiser à partir d'une autre instance. La
        // méthode "from" est masquée par FisholaMail#from donc on choisit d'introduire un nouveau nom par défaut
        from = "fromInstance")
public @interface ImmutableObject {

}
