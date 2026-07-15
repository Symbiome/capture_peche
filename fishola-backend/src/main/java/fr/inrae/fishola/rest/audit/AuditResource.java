package fr.inrae.fishola.rest.audit;

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

import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.AuditLogDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Consultation du journal d'audit depuis le module d'administration
 * (CdC 3.1.5.1 « journal global des actions »). Réservé aux admins. Lecture
 * seule et paginée ; cet endpoint n'est PAS lui-même audité (simple lecture).
 */
@Path("/api/v1/admin/audit-log")
@Produces(MediaType.APPLICATION_JSON)
public class AuditResource extends AbstractFisholaResource {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_PAGE_NUMBER = 100_000;
    private static final Set<String> ACTOR_TYPES = Set.of("admin", "user", "system");

    @Inject
    protected AuditLogDao auditLogDao;

    @GET
    public Response list(@QueryParam("actorType") String actorType,
                         @QueryParam("action") String action,
                         @QueryParam("pageNumber") @DefaultValue("0") int pageNumber,
                         @QueryParam("pageSize") @DefaultValue("50") int pageSize) {
        // Réservé aux admins (matrice §3 : « Consulter le journal global »).
        checkIsAdmin();

        Preconditions.checkArgument(pageNumber >= 0 && pageNumber <= MAX_PAGE_NUMBER,
                "pageNumber doit être dans l'intervalle [0, " + MAX_PAGE_NUMBER + "].");
        Preconditions.checkArgument(pageSize > 0 && pageSize <= MAX_PAGE_SIZE,
                "pageSize doit être dans l'intervalle [1, " + MAX_PAGE_SIZE + "].");

        Optional<String> actorFilter = Optional.ofNullable(actorType)
                .map(a -> a.trim().toLowerCase(Locale.ROOT))
                .filter(a -> !a.isEmpty());
        actorFilter.ifPresent(a -> Preconditions.checkArgument(ACTOR_TYPES.contains(a),
                "actorType doit valoir admin, user ou system."));
        Optional<String> actionFilter = Optional.ofNullable(action)
                .map(String::trim)
                .filter(a -> !a.isEmpty());

        int offset = pageNumber * pageSize;
        List<AuditLogEntry> entries = auditLogDao.list(actorFilter, actionFilter, pageSize, offset);
        return Response.ok(entries).build();
    }
}
