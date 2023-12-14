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

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import fr.inrae.fishola.database.DashboardDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.mails.FisholaMail;
import fr.inrae.fishola.mails.FisholaMailAttachment;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.mails.ImmutableFisholaMailAttachment;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.ComputedDataHolder;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource extends AbstractFisholaResource {

    @Inject
    protected Logger log;

    @Inject
    protected MailService mailService;

    @Inject
    protected DashboardDao dashboardDao;

    @Inject
    protected TripsDao tripsDao;

    protected static final ComputedDataHolder<GlobalDashboard> GLOBAL_DASHBOARD_HOLDER = new ComputedDataHolder<>();

    @GET
    @Path("/dashboard")

    public Response getDefaultPersonalDashboard(
            @QueryParam("year") Integer year,
            @QueryParam("lake") String lakeId
    ) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<Integer> yearFilter = Optional.empty();
        if (year != null) {
            yearFilter = Optional.of(year);
        }
        Optional<List<UUID>> lakesFilter = Optional.empty();
        if (lakeId != null && !lakeId.isEmpty()) {
            lakesFilter = Optional.of(Arrays.asList(UUID.fromString(lakeId)));
        }
        Dashboard result = dashboardDao.getPersonalDashboard(userId, yearFilter, lakesFilter);
        Response response = wrapEntity(result, userIdAndRenewal);
        return response;
    }

    @GET
    @Path("/global-dashboard")
    public GlobalDashboard getGlobalDashboard(
        @QueryParam("year") Integer year,
        @QueryParam("lake") String lakeId
    ) {
        // Default current dashboard : use cached value
        if (year == null && lakeId == null) {
            final GlobalDashboard result = GLOBAL_DASHBOARD_HOLDER.get(
                    this::computeNewGlobalDashboard,
                    GlobalDashboard::computedOn,
                    Duration.ofMinutes(config.globalDashboardTimeoutMinutes()),
                    false
            );
            return result;
        } else {
            Optional<Integer> yearFilter = Optional.empty();
            if (year != null) {
                yearFilter = Optional.of(year);
            }
            Optional<List<UUID>> lakesFilter = Optional.empty();
            if (lakeId != null && !lakeId.isEmpty()) {
                lakesFilter = Optional.of(Arrays.asList(UUID.fromString(lakeId)));
            }
            return this.dashboardDao.computeGlobalDashboard(yearFilter, lakesFilter, this.log);
        }
    }

    protected GlobalDashboard computeNewGlobalDashboard() {
        return this.dashboardDao.computeGlobalDashboard(Optional.empty(), Optional.empty(), this.log);
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

        ImmutableMap<String, Object> args = ImmutableMap.of(
                "firstName", user.get().getFirstName()
        );

        ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                "emails/personal-export.html",
                args);
        builder.subject("Export de données personnelles");
        builder.addTos(user.get().getEmail());

        byte[] bytes = csv.getBytes(Charsets.UTF_8);
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
