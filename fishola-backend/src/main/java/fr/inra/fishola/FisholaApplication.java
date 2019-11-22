package fr.inra.fishola;

import io.quarkus.runtime.StartupEvent;
import org.flywaydb.core.Flyway;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class FisholaApplication {

    @Inject
    protected FisholaConfiguration config;

    void onStart(@Observes StartupEvent ev) {

        Flyway flyway = Flyway.configure().dataSource(config.getJdbcUrl(), config.getJdbcUser(), config.getJdbcPassword()).load();

        flyway.migrate();

    }

}
