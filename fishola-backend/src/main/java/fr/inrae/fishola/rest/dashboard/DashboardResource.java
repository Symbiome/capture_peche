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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.entities.tables.pojos.SpeciesByLake;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import fr.inrae.fishola.mails.FisholaMail;
import fr.inrae.fishola.mails.FisholaMailAttachment;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.mails.ImmutableFisholaMailAttachment;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.ComputedDataHolder;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import fr.inrae.fishola.rest.trips.CatchBean;
import fr.inrae.fishola.rest.trips.TripResource;
import org.jboss.logging.Logger;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource extends AbstractFisholaResource {

    @Inject
    protected Logger log;

    @Inject
    protected MailService mailService;

    @Inject
    protected ReferentialDao referentialDao;

    @Inject
    protected CatchsDao catchsDao;

    @Inject
    protected TripsDao tripsDao;

    protected static final ComputedDataHolder<GlobalDashboard> GLOBAL_DASHBOARD_HOLDER = new ComputedDataHolder<>();

    @GET
    @Path("/dashboard")
    public Response getDashboard() {

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        ImmutableDashboard.Builder builder = ImmutableDashboard.builder();

        Optional<Integer> yearFilter = getYearFilter();
        Multimap<Month, Catch> monthlyCatchs = catchsDao.findMonthlyByUserId(userId, yearFilter);

        Collection<Catch> allCatches = monthlyCatchs.values();
        int allCatchsCount = allCatches.size();

        Map<UUID, Integer> caughtSpeciesCount = computeDistribution(allCatches, aCatch -> true);
        builder.caughtSpeciesCount(caughtSpeciesCount);

        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);

        Map<UUID, Integer> caughtAndReleasedSpeciesCount = computeDistribution(allCatches, aCatch -> !aCatch.getKept());
        Map<UUID, Double> caughtAndReleasedSpeciesDistribution = Maps.transformValues(caughtAndReleasedSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtAndReleasedSpeciesDistribution(caughtAndReleasedSpeciesDistribution);

        PaginationResult<DashboardLastTrip> latestTrips = computeLatestTrips(userId, allCatches);
        builder.latestTripsCatchs(latestTrips.getElements());
        int allTripsCount = (int) latestTrips.getCount();
        if (allTripsCount > 0) {
            double averageCatchsPerTrip = 1d * allCatchsCount / allTripsCount;
            builder.averageCatchsPerTrip(averageCatchsPerTrip);
        }

        Set<UUID> catchIds = allCatches.stream()
                .map(Catch::getId)
                .collect(Collectors.toSet());
        ListMultimap<UUID, Integer> catchsWithPictures = catchsDao.getPictureIndexes(catchIds);
        Set<UUID> measurementPictures = catchsDao.getMeasurementPictures(catchIds);
        Map<UUID, List<CatchBean>> topBySize = computeTopCatchs(allCatches, catchsWithPictures, measurementPictures, Catch::getSize);
        builder.topBySize(topBySize);

        Map<UUID, List<CatchBean>> topByWeight = computeTopCatchs(allCatches, catchsWithPictures, measurementPictures, Catch::getWeight);
        builder.topByWeight(topByWeight);

        List<SpeciesByLake> speciesByLakes = referentialDao.listSpeciesWithAliases();
        Multimap<UUID, SpeciesByLake> speciesWithAliasesIndex = Multimaps.index(speciesByLakes, SpeciesByLake::getSpeciesId);
        speciesWithAliasesIndex.asMap().forEach((speciesId, speciesWithAlias) -> {
            Set<String> aliases = speciesWithAlias.stream()
                    .map(SpeciesByLake::getAlias)
                    .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
            builder.putSpeciesAliases(speciesId, aliases);
        });

        builder.addOrderedMonths(Month.values());

        List<UUID> mostCaughtSpecies = get5MostCaughtSpecies(caughtSpeciesCount);

        Map<UUID, Map<Month, Double>> monthlySizes = computeMonthlySizes(mostCaughtSpecies, monthlyCatchs);
        builder.monthlySizes(monthlySizes);

        Dashboard result = builder.build();
        Response response = wrapEntity(result, userIdAndRenewal);
        return response;
    }

    protected Optional<Integer> getYearFilter() {
        Optional<Integer> result = config.dashboardOnlyCurrentYear()
                ? Optional.of(Year.now().getValue())
                : Optional.empty();
        return result;
    }

    /**
     * Calcule la moyenne mensuelle des tailles de poissons pour les espèces spécifiées
     */
    protected Map<UUID, Map<Month, Double>> computeMonthlySizes(List<UUID> mostCaughtSpecies, Multimap<Month, Catch> monthlyCatches) {
        Map<UUID, Month> catchesMonths = monthlyCatches.entries().stream().collect(Collectors.toMap(e -> e.getValue().getId(), Map.Entry::getKey));
        Map<UUID, Map<Month, Double>> result = computeMonthlySizes(catchesMonths, mostCaughtSpecies, monthlyCatches.values());
        return result;
    }

    protected Map<UUID, Map<Month, Double>> computeMonthlySizes(Map<UUID, Month> catchesMonths, List<UUID> mostCaughtSpecies, Collection<Catch> allCatches) {
        // On garde uniquement les captures avec une taille
        List<Catch> catchsWithSize = allCatches.stream()
                .filter(c -> c.getSize() != null)
                .collect(Collectors.toList());
        ImmutableMap.Builder<UUID, Map<Month, Double>> builder = ImmutableMap.builder();
        for (UUID speciesId : mostCaughtSpecies) {
            // On prend les captures de la bonne espèce
            List<Catch> catchs = catchsWithSize.stream()
                    .filter(c -> c.getSpeciesId().equals(speciesId))
                    .collect(Collectors.toList());
            Map<Month, Double> speciesMonthlySizes = new HashMap<>();
            for (Month month : Month.values()) {
                OptionalDouble average = catchs.stream()
                        .filter(c -> month.equals(catchesMonths.get(c.getId())))
                        .mapToInt(Catch::getSize)
                        .average();
                average.ifPresent(val -> speciesMonthlySizes.put(month, val));
            }
            if (!speciesMonthlySizes.isEmpty()) {
                builder.put(speciesId, speciesMonthlySizes);
            }
        }
        Map<UUID, Map<Month, Double>> result = builder.build();
        return result;
    }

    /**
     * Calcule les 5 espèces les plus capturées à partir de la distribution fournie
     */
    protected List<UUID> get5MostCaughtSpecies(Map<UUID, Integer> caughtSpeciesCount) {
        Map<UUID, Integer> copy = new HashMap<>(caughtSpeciesCount);
        List<UUID> result = new LinkedList<>();
        while (result.size() < 5 && !copy.isEmpty()) {
            List<UUID> subList = getMostCaughtSpecies(copy);
            result.addAll(subList);
            subList.forEach(copy::remove);
        }
        return result;
    }

    /**
     * Calcule la (ou les si ex-aequo) espèce(s) la(es) plus capturée(s)
     */
    protected List<UUID> getMostCaughtSpecies(Map<UUID, Integer> caughtSpeciesCount) {
        List<UUID> result = new LinkedList<>();
        int max = 0;
        for (Map.Entry<UUID, Integer> entry : caughtSpeciesCount.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                result.clear();
                result.add(entry.getKey());
            } else if (entry.getValue() == max) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    protected List<CatchBean> toDashboardTopCatchs(Collection<Catch> catches,
                                                   Ordering<Catch> ordering,
                                                   ListMultimap<UUID, Integer> catchsWithPictures,
                                                   Set<UUID> measurementPictures) {
        List<CatchBean> result = ordering.immutableSortedCopy(catches)
                .stream()
                .limit(5)
                .map(aCatch -> TripResource.toCatchBean(aCatch, catchsWithPictures, measurementPictures))
                .collect(Collectors.toList());
        return result;
    }

    protected Map<UUID, List<CatchBean>> computeTopCatchs(Collection<Catch> allCatches,
                                                          ListMultimap<UUID, Integer> catchsWithPictures,
                                                          Set<UUID> measurementPictures,
                                                          Function<Catch, Integer> getter) {
        Multimap<UUID, Catch> catchsBySpecies = Multimaps.index(allCatches, Catch::getSpeciesId);
        // On commence par retirer les captures dont la valeur est nulle
        catchsBySpecies = Multimaps.filterValues(catchsBySpecies, aCatch -> getter.apply(aCatch) != null);

        Ordering<Catch> ordering = Ordering.natural()
                .onResultOf(getter::apply)
                .reverse();

        Map<UUID, List<CatchBean>> result = new HashMap<>();
        catchsBySpecies.asMap()
                .forEach((key, value) -> {
                    List<CatchBean> topCatchs = toDashboardTopCatchs(value, ordering, catchsWithPictures, measurementPictures);
                    result.put(key, topCatchs);
                });
        return result;
    }

    protected Map<UUID, Integer> computeDistribution(Collection<Catch> allCatches, Predicate<Catch> predicate) {
        ImmutableMultiset<UUID> caughtSpeciesDistributionSet = allCatches.stream()
                .filter(predicate)
                .map(Catch::getSpeciesId)
                .collect(ImmutableMultiset.toImmutableMultiset());
        Map<UUID, Integer> result = caughtSpeciesDistributionSet.entrySet()
                .stream()
                .collect(Collectors.toMap(Multiset.Entry::getElement, Multiset.Entry::getCount));
        return result;
    }

    protected PaginationResult<DashboardLastTrip> computeLatestTrips(UUID userId, Collection<Catch> allCatches) {
        ImmutableListMultimap<UUID, Catch> allCatchsIndex = Multimaps.index(allCatches, Catch::getTripId);
        PaginationParameter page = PaginationParameter.builder(0, 9)
                .addOrder("date", true)
                .build();
        Optional<Integer> yearFilter = getYearFilter();
        PaginationResult<Trip> latestTripsEntities = tripsDao.listMyTrips(userId, page, Optional.empty(), yearFilter);
        PaginationResult<DashboardLastTrip> result = latestTripsEntities.transform(trip -> toDashboardLastTrip(trip, allCatchsIndex));
        return result;
    }

    protected DashboardLastTrip toDashboardLastTrip(Trip trip, ImmutableListMultimap<UUID, Catch> allCatchsIndex) {
        DashboardLastTrip result = ImmutableDashboardLastTrip.builder()
                .tripId(trip.getId())
                .catchsCount(allCatchsIndex.get(trip.getId()).size())
                .day(trip.getDay())
                .build();
        return result;
    }

    @GET
    @Path("/global-dashboard")
    public GlobalDashboard getGlobalDashboard() {

        final GlobalDashboard result = GLOBAL_DASHBOARD_HOLDER.get(
                this::computeNewGlobalDashboard,
                GlobalDashboard::computedOn,
                Duration.ofMinutes(config.globalDashboardTimeoutMinutes()),
                false
        );

        return result;
    }

    protected GlobalDashboard computeNewGlobalDashboard() {

        ImmutableGlobalDashboard.Builder builder = ImmutableGlobalDashboard.builder();

        Optional<Integer> yearFilter = getYearFilter();
        Multimap<Month, Catch> monthlyCatchs = catchsDao.findAll(yearFilter);

        Collection<Catch> allCatches = monthlyCatchs.values();
        int allCatchsCount = allCatches.size();

        Map<UUID, Integer> caughtSpeciesCount = computeDistribution(allCatches, aCatch -> true);
        builder.caughtSpeciesCount(caughtSpeciesCount);

        Map<UUID, Double> caughtSpeciesDistribution = Maps.transformValues(caughtSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtSpeciesDistribution(caughtSpeciesDistribution);

        Map<UUID, Integer> caughtAndReleasedSpeciesCount = computeDistribution(allCatches, aCatch -> !aCatch.getKept());
        Map<UUID, Double> caughtAndReleasedSpeciesDistribution = Maps.transformValues(caughtAndReleasedSpeciesCount, count -> count * 100d / allCatchsCount);
        builder.caughtAndReleasedSpeciesDistribution(caughtAndReleasedSpeciesDistribution);

        List<SpeciesByLake> speciesByLakes = referentialDao.listSpeciesWithAliases();
        Multimap<UUID, SpeciesByLake> speciesWithAliasesIndex = Multimaps.index(speciesByLakes, SpeciesByLake::getSpeciesId);
        speciesWithAliasesIndex.asMap().forEach((speciesId, speciesWithAlias) -> {
            Set<String> aliases = speciesWithAlias.stream()
                    .map(SpeciesByLake::getAlias)
                    .collect(ImmutableSortedSet.toImmutableSortedSet(Ordering.natural()));
            builder.putSpeciesAliases(speciesId, aliases);
        });

        builder.addOrderedMonths(Month.values());

        List<UUID> mostCaughtSpecies = get5MostCaughtSpecies(caughtSpeciesCount);

        Map<UUID, Map<Month, Double>> monthlySizes = computeMonthlySizes(mostCaughtSpecies, monthlyCatchs);
        builder.monthlySizes(monthlySizes);

        builder.computedOn(LocalDateTime.now());

        GlobalDashboard result = builder.build();

        if (log.isDebugEnabled()) {
            log.debugf("Nouvelle instance: %s", result);
        }
        return result;
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
