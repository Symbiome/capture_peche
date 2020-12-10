package fr.inrae.fishola.rest.about;

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Path("/api/v1/about")
@Produces(MediaType.APPLICATION_JSON)
public class AboutResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(AboutResource.class);

    protected static KeyFigures latestKeyFigures;
    protected static AtomicBoolean runningRefresh = new AtomicBoolean(false);

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;
    @Inject
    protected CatchsDao catchsDao;

    protected KeyFigures computeKeyFigures() {
        if (log.isDebugEnabled()) {
            log.debug("Nouveau calcul en cours");
        }
        ImmutableKeyFigures.Builder builder = ImmutableKeyFigures.builder();
        List<Lake> lakes = referentialDao.listLakes();
        int tripsCount = tripsDao.countTrips();
        int catchsCount = catchsDao.countCatchs();
        int picturesCount = catchsDao.countPictures();
        builder.tripsCount(tripsCount)
                .catchsCount(catchsCount)
                .picturesCount(picturesCount)
                .lakes(lakes)
                .titleText("TITLE TEXT") // TODO AThimel 10/12/2020 est l'application smartphone pour une gestion durable de la pêche sur les lacs alpins (Léman, lac d’Annecy, du Bourget et d’Aiguebelette).
                .contributeText("CONTRIBUTE TEXT")// TODO AThimel 10/12/2020
                .computedOn(LocalDateTime.now());
        KeyFigures result = builder.build();
        if (log.isDebugEnabled()) {
            log.debug("Nouvelle instance: " + result);
        }
        return result;
    }

    protected void asyncRefreshKeyFigures() {
        boolean isCurrentlyRunning = runningRefresh.compareAndExchange(false, true);
        if (!isCurrentlyRunning) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                latestKeyFigures = computeKeyFigures();
                runningRefresh.set(false);
            });
        } else {
            System.out.println("Refresh already pending, skipping");
        }
    }

    @GET
    @Path("/key-figures")
    public KeyFigures getKeyFigures() {

        if (latestKeyFigures == null) {
            latestKeyFigures = computeKeyFigures();
        }

        Duration age = Duration.between(latestKeyFigures.computedOn(), LocalDateTime.now());
        if (age.toSeconds() > 6) {
            // TODO AThimel 10/12/2020 Mettre un vrai délai + conf
            asyncRefreshKeyFigures();
        }

        final KeyFigures result = latestKeyFigures;
        return result;
    }
}
