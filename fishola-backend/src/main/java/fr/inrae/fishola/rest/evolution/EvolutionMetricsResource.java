package fr.inrae.fishola.rest.evolution;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.database.EvolutionDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.FisholaCache;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import fr.inrae.fishola.rest.dashboard.EvolutionMetricsForWaterEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Optional;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class EvolutionMetricsResource extends AbstractFisholaResource {

    @Inject
    private EvolutionDao evolutionDao;

    @Inject
    private FisholaCache cache;

    /**
     * Returns for the given waterEntity a map having :
     * - One entry per year
     * - For each year, on entry per specie id
     * - For each specie, one entry per month
     * - For each month a pair representing count and avg size of the given specie
     */
    @GET
    @Path("/evolution/global/{waterEntityId}")
    public EvolutionMetricsForWaterEntity getGlobalEvolutionStats(
            @PathParam("waterEntityId") UUID waterEntityId
    ) {
        return cache.globalEvolution.get(waterEntityId.toString(), key -> evolutionDao.getEvolutionStatsForWaterEntity(waterEntityId, Optional.empty()));
    }


    @GET
    @Path("/evolution/personal/{waterEntityId}")
    public EvolutionMetricsForWaterEntity getPersonalEvolutionStats(
            @PathParam("waterEntityId") UUID waterEntityId
    ) {
        UserIdAndRenewal userId = getUserIdOrRenew();
        String cacheKey  = waterEntityId.toString() + "_" + userId.userId().toString();
        return cache.personalEvolution.get(cacheKey, key -> evolutionDao.getEvolutionStatsForWaterEntity(waterEntityId, Optional.of(userId.userId())));
    }
}
