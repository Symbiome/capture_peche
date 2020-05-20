package fr.inrae.fishola;

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

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        flyway.migrate();

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
                    if (log.isWarnEnabled()) {
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

}
