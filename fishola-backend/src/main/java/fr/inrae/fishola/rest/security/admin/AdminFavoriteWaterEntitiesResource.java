package fr.inrae.fishola.rest.security.admin;

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

import fr.inrae.fishola.database.AdminFavoritesDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.referential.WaterEntityName;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Secteurs favoris du compte staff connecté (#236) : proposés en tête de la recherche de
 * secteur des saisies opérateur. Propres à chaque compte, hors national limités à son
 * périmètre départemental.
 */
@Path("/api/v1/admin/favorite-water-entities")
@Produces(MediaType.APPLICATION_JSON)
public class AdminFavoriteWaterEntitiesResource extends AbstractFisholaResource {

    @Inject
    protected AdminFavoritesDao adminFavoritesDao;

    /**
     * Favoris du compte connecté.
     *
     * @return secteurs favoris, triés par nom
     */
    @GET
    public List<WaterEntityName> list() {
        FisholaAdmin admin = checkIsStaff();
        return adminFavoritesDao.listFavorites(admin.getId());
    }

    /**
     * Ajoute un secteur aux favoris du compte connecté.
     *
     * @param waterEntityId secteur à ajouter
     * @return 204, 404 si le secteur est inconnu, 403 s'il est hors périmètre
     */
    @PUT
    @Path("/{waterEntityId}")
    public Response add(@PathParam("waterEntityId") UUID waterEntityId) {
        FisholaAdmin admin = checkIsStaff();
        if (!adminFavoritesDao.existsWaterEntity(waterEntityId)) {
            throw new NotFoundException();
        }
        if (!admin.getIsNationalAdmin()
                && !adminDao.getAllowedWaterEntityIds(admin.getId()).contains(waterEntityId)) {
            throw new ForbiddenException("Secteur hors de votre périmètre");
        }
        adminFavoritesDao.addFavorite(admin.getId(), waterEntityId);
        return Response.noContent().build();
    }

    /**
     * Retire un secteur des favoris du compte connecté.
     *
     * @param waterEntityId secteur à retirer
     * @return 204, même si le secteur n'était pas favori
     */
    @DELETE
    @Path("/{waterEntityId}")
    public Response remove(@PathParam("waterEntityId") UUID waterEntityId) {
        FisholaAdmin admin = checkIsStaff();
        adminFavoritesDao.removeFavorite(admin.getId(), waterEntityId);
        return Response.noContent().build();
    }
}
