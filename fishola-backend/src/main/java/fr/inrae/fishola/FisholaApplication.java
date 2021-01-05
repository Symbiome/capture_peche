package fr.inrae.fishola;

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
import java.util.List;
import java.util.Map;

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

        checkForDocumentations();
    }

    @Transactional
    void checkForDocumentations() {
        List<Documentation> emptyDocumentations = documentationDao.listDocumentationsWithoutContent();

        ImmutableMap<String, String> defaultPDFs = ImmutableMap.<String, String>builder()
                .put("annecy", "/sample/reglement-annecy.pdf")
                .put("léman", "/sample/reglement-leman.pdf")
                .put("bourget", "/sample/reglement-bourget.pdf")
                .put("aiguebelette", "/sample/reglement-aiguebelette.pdf")
                .put("espèces", "/sample/presentation-coregone-final.pdf")
                .put("prélèvements", "/sample/fiche-prelevement.pdf")
                .put("cgu", "/sample/CGU.pdf")
                .build();

        if (!emptyDocumentations.isEmpty()) {
            try {
                for (Documentation documentation : emptyDocumentations) {
                    String path = defaultPDFs.getOrDefault(documentation.getNaturalId(), "/sample/documentation.pdf");
                    InputStream resource = this.getClass().getResourceAsStream(path);
                    byte[] bytes = IOUtils.toByteArray(resource);
                    if (log.isInfoEnabled()) {
                        log.info("Insertion du PDF par défaut : " + documentation.getName());
                    }
                    documentation.setContent(bytes);
                    documentationDao.updateDocumentation(documentation);
                }
            } catch (IOException ioe) {
                throw new FisholaTechnicalException("Impossible de lire le fichier par défaut", ioe);
            }
        }
    }

}
