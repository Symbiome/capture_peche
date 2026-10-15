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

import fr.inrae.fishola.database.AdminHiddenReferentialsDao;
import fr.inrae.fishola.entities.enums.HideableReferential;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.audit.Audited;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Éléments du référentiel national masqués dans les listes de saisie du compte staff
 * connecté (#239), un référentiel à la fois ({@code species}, {@code technique}). Propres
 * à chaque compte ; le référentiel national et l'import de fichiers ne sont pas affectés.
 */
@Path("/api/v1/admin/hidden-referentials/{type}")
@Produces(MediaType.APPLICATION_JSON)
public class AdminHiddenReferentialsResource extends AbstractFisholaResource {

    @Inject
    protected AdminHiddenReferentialsDao hiddenReferentialsDao;

    /**
     * Éléments masqués par le compte connecté.
     *
     * @param type référentiel : {@code species} ou {@code technique}
     * @return identifiants masqués ; 404 si le référentiel ne se masque pas
     */
    @GET
    public Set<UUID> list(@PathParam("type") String type) {
        FisholaAdmin admin = checkIsStaff();
        return hiddenReferentialsDao.listHidden(admin.getId(), referentialOf(type));
    }

    /**
     * Remplace les éléments masqués par le compte connecté.
     *
     * @param type référentiel : {@code species} ou {@code technique}
     * @param itemIds éléments à masquer, liste vide pour tout afficher
     * @return 204 ; 400 si un identifiant est inconnu du référentiel, 404 si le référentiel ne se masque pas
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Audited(value = "hiddenReferential.update", entityType = "fishola_admin_hidden_referential")
    public Response replace(@PathParam("type") String type, List<UUID> itemIds) {
        FisholaAdmin admin = checkIsStaff();
        HideableReferential referential = referentialOf(type);
        Set<UUID> hidden = itemIds == null ? Set.of() : new HashSet<>(itemIds);
        Set<UUID> unknown = hiddenReferentialsDao.unknownItemIds(referential, hidden);
        if (!unknown.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Éléments inconnus du référentiel : " + unknown)
                    .build();
        }
        hiddenReferentialsDao.replaceHidden(admin.getId(), referential, hidden);
        return Response.noContent().build();
    }

    private static HideableReferential referentialOf(String type) {
        HideableReferential referential = HideableReferential.lookupLiteral(type);
        if (referential == null) {
            throw new NotFoundException("Référentiel non personnalisable : " + type);
        }
        return referential;
    }
}
