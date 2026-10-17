package fr.inrae.fishola.rest.gamification;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Carte souvenir d'un déblocage (#275), renvoyée par {@link GamificationResource#mySouvenir} :
 * les prises « preuves » du badge résolues en données affichables. Les captures suivent
 * l'ordre des preuves (la première est la prise mise en avant) ; les sorties comprennent
 * celles des captures retenues. Listes vides quand il ne reste rien à montrer (badge sans
 * prise associée, prises supprimées depuis) : le mobile affiche alors la carte générique.
 */
public class BadgeSouvenirBean {

    public UUID unlockId;
    public List<SouvenirCatchBean> catches = new ArrayList<>();
    public List<SouvenirTripBean> trips = new ArrayList<>();
}
