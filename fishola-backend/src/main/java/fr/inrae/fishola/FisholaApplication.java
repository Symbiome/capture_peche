package fr.inrae.fishola;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import com.google.common.collect.ImmutableMap;
import fr.inrae.fishola.database.EditorialAndDocumentationDao;
import fr.inrae.fishola.entities.tables.pojos.Documentation;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flywaydb.core.Flyway;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FisholaApplication {

    private static final Log log = LogFactory.getLog(FisholaApplication.class);

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected AgroalDataSource dataSource;

    @Inject
    protected EditorialAndDocumentationDao documentationDao;

    void onStart(@Observes StartupEvent ev) {

        if (log.isInfoEnabled()) {
            log.info(String.format("Starting Fishola version='%s' ; profile=%s", config.getFullVersion(), config.getActiveProfile()));
        }

        Map<String, String> flywayPlaceholders = ImmutableMap.of(
                "baseUrl", config.getBackendBaseUrl().orElse("http://localhost:8080"),
                "exportSafeHours", String.valueOf(config.getExportSafeHours())
        );
        Flyway flyway = Flyway.configure()
                .placeholders(flywayPlaceholders)
                .dataSource(dataSource)
                .load();

        flyway.migrate();

        ensureDocumentations();
        checkForDocumentations();
    }

    @Transactional
    void checkForDocumentations() {
        List<Documentation> emptyDocumentations = documentationDao.listDocumentationsWithoutContent();
        if (!emptyDocumentations.isEmpty()) {
            InputStream resource = this.getClass().getResourceAsStream("/sample/documentation.pdf");
            try {
                byte[] bytes = IOUtils.toByteArray(resource);
                for (Documentation documentation : emptyDocumentations) {
                    if (log.isInfoEnabled()) {
                        log.info("Insertion du PDF de remplacement : " + documentation.getName());
                    }
                    documentation.setContent(bytes);
                    documentationDao.updateDocumentation(documentation);
                }
            } catch (IOException ioe) {
                throw new FisholaTechnicalException("Impossible de lire le fichier par défaut", ioe);
            }
        }
    }

    @Transactional
    void ensureDocumentations() {
        ensureDocumentationByName("Réglementation sur le lac d'Annecy", "/sample/reglement-annecy.pdf");
        ensureDocumentationByName("Réglementation sur le Léman", "/sample/reglement-leman.pdf");
        ensureDocumentationByName("Réglementation sur le lac du Bourget", "/sample/reglement-bourget.pdf");
        ensureDocumentationByName("Réglementation sur le lac d'Aiguebelette", "/sample/reglement-aiguebelette.pdf");
        ensureDocumentationByName("Documentation sur les espèces", "/sample/presentation-coregone-final.pdf");
        ensureDocumentationByName("Documentation sur les prélèvements", "/sample/fiche-prelevement.pdf");
        ensureDocumentationByName("Conditions Générales d'Utilisation", "/sample/CGU.pdf");
    }

    @Transactional
    void ensureDocumentationByName(String name, String resourcePath) {

        LinkedHashMap<UUID, String> docs = documentationDao.listDocumentations();

        // TODO AThimel 09/04/2020 Améliorer ça pour éviter les problèmes en cas de renommage inopiné
        Optional<UUID> docId = docs.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findAny();

        if (docId.isPresent()) {

            Optional<Documentation> optional = documentationDao.getDocumentation(docId.get());
            if (optional.isPresent()) {
                InputStream resource = this.getClass().getResourceAsStream(resourcePath);
                try {
                    byte[] bytes = IOUtils.toByteArray(resource);
                    Documentation documentation = optional.get();
                    if (log.isInfoEnabled()) {
                        log.info("Insertion du PDF : " + documentation.getName());
                    }
                    documentation.setContent(bytes);
                    documentationDao.updateDocumentation(documentation);
                } catch (IOException ioe) {
                    throw new FisholaTechnicalException("Impossible de lire le fichier", ioe);
                }
            } else {
                log.error("Unable to find documentation in database");
            }

        } else {
            log.error("Unable to find documentation in database");
        }
    }

}
