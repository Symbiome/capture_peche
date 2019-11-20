package fr.inra.fishola;

import io.quarkus.runtime.StartupEvent;
import org.flywaydb.core.Flyway;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class FisholaApplication {

    void onStart(@Observes StartupEvent ev) {

        String url = "jdbc:postgresql://172.17.0.2/fishola";
        String user = "postgres";
        String password = null;

        Flyway flyway = Flyway.configure().dataSource(url, user, password).load();

        flyway.migrate();

    }

}
