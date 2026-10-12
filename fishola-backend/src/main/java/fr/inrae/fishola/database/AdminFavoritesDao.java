package fr.inrae.fishola.database;

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

import fr.inrae.fishola.rest.referential.ImmutableWaterEntityName;
import fr.inrae.fishola.rest.referential.WaterEntityName;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.inrae.fishola.entities.Tables.FISHOLA_ADMIN_FAVORITE_WATER_ENTITY;
import static fr.inrae.fishola.entities.Tables.WATER_ENTITY;

/**
 * Secteurs favoris propres à chaque compte opérateur (#236), proposés en tête de la
 * recherche de secteur, sur le modèle des favoris pêcheur de {@link UsersDao}.
 */
@Singleton
public class AdminFavoritesDao extends AbstractFisholaDao {

    /**
     * Favoris d'un compte, triés par nom.
     *
     * @param adminId compte opérateur
     * @return identifiant, nom et département de chaque secteur favori
     */
    public List<WaterEntityName> listFavorites(UUID adminId) {
        return withContext(context -> context
                .select(WATER_ENTITY.ID, WATER_ENTITY.NAME, WATER_ENTITY.DEPARTMENT)
                .from(WATER_ENTITY)
                .join(FISHOLA_ADMIN_FAVORITE_WATER_ENTITY)
                .on(FISHOLA_ADMIN_FAVORITE_WATER_ENTITY.WATER_ENTITY_ID.eq(WATER_ENTITY.ID))
                .where(FISHOLA_ADMIN_FAVORITE_WATER_ENTITY.FISHOLA_ADMIN_ID.eq(adminId))
                .orderBy(WATER_ENTITY.NAME)
                .fetch(rec -> (WaterEntityName) ImmutableWaterEntityName.builder()
                        .id(rec.get(WATER_ENTITY.ID))
                        .name(rec.get(WATER_ENTITY.NAME))
                        .department(Optional.ofNullable(rec.get(WATER_ENTITY.DEPARTMENT)))
                        .build()));
    }

    /**
     * Le secteur existe-t-il dans le référentiel hydro ?
     *
     * @param waterEntityId secteur recherché
     * @return vrai s'il existe
     */
    public boolean existsWaterEntity(UUID waterEntityId) {
        return withContext(context -> context.fetchExists(WATER_ENTITY, WATER_ENTITY.ID.eq(waterEntityId)));
    }

    /**
     * Ajoute un secteur aux favoris du compte ; sans effet s'il y est déjà.
     *
     * @param adminId compte opérateur
     * @param waterEntityId secteur à ajouter
     */
    public void addFavorite(UUID adminId, UUID waterEntityId) {
        withContextNoResult(context -> context
                .insertInto(FISHOLA_ADMIN_FAVORITE_WATER_ENTITY,
                        FISHOLA_ADMIN_FAVORITE_WATER_ENTITY.FISHOLA_ADMIN_ID,
                        FISHOLA_ADMIN_FAVORITE_WATER_ENTITY.WATER_ENTITY_ID)
                .values(adminId, waterEntityId)
                .onConflictDoNothing()
                .execute());
    }

    /**
     * Retire un secteur des favoris du compte ; sans effet s'il n'y est pas.
     *
     * @param adminId compte opérateur
     * @param waterEntityId secteur à retirer
     */
    public void removeFavorite(UUID adminId, UUID waterEntityId) {
        withContextNoResult(context -> context
                .deleteFrom(FISHOLA_ADMIN_FAVORITE_WATER_ENTITY)
                .where(FISHOLA_ADMIN_FAVORITE_WATER_ENTITY.FISHOLA_ADMIN_ID.eq(adminId))
                .and(FISHOLA_ADMIN_FAVORITE_WATER_ENTITY.WATER_ENTITY_ID.eq(waterEntityId))
                .execute());
    }
}
