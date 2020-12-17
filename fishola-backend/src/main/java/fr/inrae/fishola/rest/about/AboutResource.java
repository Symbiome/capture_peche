package fr.inrae.fishola.rest.about;

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.EditorialAndDocumentationDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Editorial;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Path("/api/v1/about")
@Produces(MediaType.APPLICATION_JSON)
public class AboutResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(AboutResource.class);

    /**
     * Dernière instance de KeyFigures calculée. Celle-ci va rester en mémoire jusqu'à ce qu'elle expire (cf délai dans
     * la configuration).
     */
    protected static KeyFigures latestKeyFigures;

    /**
     * Permet de ne pas faire plusieurs fois le calcul en même temps.
     */
    protected static AtomicBoolean runningRefresh = new AtomicBoolean(false);

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;
    @Inject
    protected CatchsDao catchsDao;
    @Inject
    protected EditorialAndDocumentationDao editorialAndDocumentationDao;

    /**
     * Procède au calcul d'une nouvelle instance de KeyFigures
     *
     * @return l'instance fraichement calculée
     */
    protected KeyFigures computeKeyFigures() {
        ImmutableKeyFigures.Builder builder = ImmutableKeyFigures.builder();
        int tripsCount = tripsDao.countTrips();
        int catchsCount = catchsDao.countCatchs();
        Map<UUID, Integer> countCatchsByLakeId = catchsDao.countCatchsByLakeId();
        int picturesCount = catchsDao.countPictures();
        List<Lake> lakes = referentialDao.listLakes();
        Optional<Editorial> title = editorialAndDocumentationDao.findEditorial("about_title");
        Optional<Editorial> contribute = editorialAndDocumentationDao.findEditorial("about_contribute");

        builder.tripsCount(tripsCount)
                .catchsCount(catchsCount)
                .picturesCount(picturesCount)
                .lakes(lakes)
                .catchsCountPerLakeId(countCatchsByLakeId)
                .titleText(title.map(Editorial::getContent).orElse("N/A"))
                .contributeText(contribute.map(Editorial::getContent).orElse("N/A"))
                .computedOn(LocalDateTime.now());
        KeyFigures result = builder.build();

        if (log.isDebugEnabled()) {
            log.debug("Nouvelle instance: " + result);
        }
        return result;
    }

    /**
     * Méthode non bloquante qui déclenche la mise à jour de l'instance de KeyFigures mais qui n'attend pas que son
     * calcul soit terminé avant de rendre la main
     */
    protected void asyncRefreshKeyFigures() {
        boolean isCurrentlyRunning = runningRefresh.compareAndExchange(false, true);
        if (!isCurrentlyRunning) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                latestKeyFigures = computeKeyFigures();
                runningRefresh.set(false);
            });
        }
    }

    @GET
    @Path("/key-figures")
    public KeyFigures getKeyFigures() {

        if (latestKeyFigures == null) {
            latestKeyFigures = computeKeyFigures();
        }

        // On calcule l'age de l'instance actuelle.
        Duration age = Duration.between(latestKeyFigures.computedOn(), LocalDateTime.now());
        // Si elle a expiré on demandé son calcul en arrière-plan
        if (age.toHours() >= config.getKeyFiguresTimeoutHours()) {
            asyncRefreshKeyFigures();
        }

        // Dans tous les cas on renvoie l'instance partagée (même si elle est peut-être expirée)
        final KeyFigures result = latestKeyFigures;
        return result;
    }

}
