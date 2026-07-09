package fr.inrae.fishola.rest.dashboard;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.database.DashboardDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.mails.FisholaMail;
import fr.inrae.fishola.mails.FisholaMailAttachment;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.mails.ImmutableFisholaMailAttachment;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.FisholaCache;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource extends AbstractFisholaResource {

    @Inject
    protected MailService mailService;

    @Inject
    protected DashboardDao dashboardDao;

    @Inject
    protected TripsDao tripsDao;

    @Inject
    protected FisholaCache cache;

    @GET
    @Path("/dashboard")

    public Response getDefaultPersonalDashboard(
            @QueryParam("year") Integer year,
            @QueryParam("waterEntity") String waterEntityId
    ) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<Integer> yearFilter = Optional.empty();
        if (year != null) {
            yearFilter = Optional.of(year);
        }
        Optional<List<UUID>> waterEntitiesFilter = Optional.empty();
        if (waterEntityId != null && !waterEntityId.isEmpty()) {
            waterEntitiesFilter = Optional.of(Arrays.asList(UUID.fromString(waterEntityId)));
        }
        Dashboard result = dashboardDao.getPersonalDashboard(userId, yearFilter, waterEntitiesFilter);
        Response response = wrapEntity(result, userIdAndRenewal);
        return response;
    }

    @GET
    @Path("/global-dashboard")
    public GlobalDashboard getGlobalDashboard(
        @QueryParam("year") Integer year,
        @QueryParam("waterEntity") UUID waterEntityId
    ) {
        if (year == null || waterEntityId == null) {
            throw new IllegalArgumentException("Dashboard need a year and a waterEntityId to be computed, got " + year + " and " + waterEntityId);
        }
        Optional<Integer> yearFilter = Optional.of(year);
        Optional<List<UUID>> waterEntitiesFilter = Optional.of(List.of(waterEntityId));
        String cacheKey = year + "_" + waterEntityId;
        return cache.globalDashboard.get(cacheKey, key -> this.dashboardDao.computeGlobalDashboard(yearFilter, waterEntitiesFilter));
    }


    @GET
    @Path("/dashboard/export")
    @Produces("text/csv")
    public Response exportAsCSV() {

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        String csv = tripsDao.getPersonalTripsCSV(userId);
        String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String disposition = String.format("filename=\"Fishola_Export_%s.csv\"", dateFormatted);

        Response.ResponseBuilder responseBuilder = Response.ok(csv)
                .header("Content-Disposition", disposition);
        Response response = buildResponse(responseBuilder, userIdAndRenewal);
        return response;
    }

    @POST
    @Path("/dashboard/async-export")
    public Response asyncExportAsCSV() {

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        String csv = tripsDao.getPersonalTripsCSV(userId);

        String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = String.format("Fishola_Export_%s.csv", dateFormatted);

        FisholaMail fisholaMail = toFisholaMail(userId, fileName, csv);
        mailService.sendMail(fisholaMail);

        Response.ResponseBuilder responseBuilder = Response.ok();
        Response response = buildResponse(responseBuilder, userIdAndRenewal);
        return response;
    }

    protected FisholaMail toFisholaMail(UUID userId, String fileName, String csv) {

        Optional<FisholaUser> user = usersDao.findById(userId);
        Preconditions.checkState(user.isPresent());

        Map<String, Object> args = Map.of(
                "firstName", user.get().getFirstName()
                                         );

        ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                "emails/personal-export.html",
                args);
        builder.subject("Export de données personnelles");
        builder.addTos(user.get().getEmail());

        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        FisholaMailAttachment attachment = ImmutableFisholaMailAttachment.builder()
                .bytes(bytes)
                .name(fileName)
                .type(com.google.common.net.MediaType.CSV_UTF_8)
                .build();
        builder.addAttachments(attachment);

        FisholaMail result = builder.build();
        return result;
    }

}
