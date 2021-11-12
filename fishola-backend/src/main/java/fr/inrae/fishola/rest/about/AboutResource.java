package fr.inrae.fishola.rest.about;

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

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.EditorialAndDocumentationDao;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.Editorial;
import fr.inrae.fishola.entities.tables.pojos.Lake;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.ComputedDataHolder;
import org.jboss.logging.Logger;

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

@Path("/api/v1/about")
@Produces(MediaType.APPLICATION_JSON)
public class AboutResource extends AbstractFisholaResource {

    @Inject
    protected Logger log;

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected TripsDao tripsDao;
    @Inject
    protected CatchsDao catchsDao;
    @Inject
    protected EditorialAndDocumentationDao editorialAndDocumentationDao;

    public static final ComputedDataHolder<KeyFigures> KEY_FIGURES_HOLDER = new ComputedDataHolder<>();

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
            log.debugf("Nouvelle instance: %s", result);
        }
        return result;
    }

    @GET
    @Path("/key-figures")
    public KeyFigures getKeyFigures() {

        final KeyFigures result = KEY_FIGURES_HOLDER.get(
                this::computeKeyFigures,
                KeyFigures::computedOn,
                Duration.ofHours(config.keyFiguresTimeoutHours()),
                true
        );

        return result;
    }

}
