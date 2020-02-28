package fr.inra.fishola;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import org.flywaydb.core.Flyway;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class FisholaApplication {

    @Inject
    protected AgroalDataSource dataSource;

    void onStart(@Observes StartupEvent ev) {

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();

        flyway.migrate();

    }

}
