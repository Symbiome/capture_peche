package fr.inrae.fishola.rest.trips;

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

import com.google.common.collect.ImmutableSet;
import fr.inrae.fishola.database.ReferentialDao;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.AbstractFisholaTest;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Une sortie saisie dans l'application expose son origine et les codes SANDRE de ses
 * espèces dans les vues d'export (#235). Lue dans catchs_pending_validation, qui n'a pas
 * l'embargo de 7 jours de catchs_openadom_export sur les saisies pêcheur.
 */
@QuarkusTest
class TripOriginExportTest extends AbstractFisholaTest {

    @Inject
    protected ReferentialDao referentialDao;
    @Inject
    protected AgroalDataSource dataSource;

    protected List<Species> codedSpecies;
    protected UUID techniqueId;
    protected UUID waterEntityId;

    @BeforeEach
    @Transactional
    void loadReferentialsAndGiveSpeciesTestCodes() {
        this.codedSpecies = referentialDao.listBuiltInSpecies().stream().limit(2).toList();
        this.techniqueId = referentialDao.listBuiltInTechniques().iterator().next().getId();
        this.waterEntityId = referentialDao.listWaterEntities().iterator().next().getId();
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("UPDATE species SET code_espece = 'ZZA' WHERE id = ?", codedSpecies.get(0).getId());
        ctx.execute("UPDATE species SET code_espece = 'ZZB' WHERE id = ?", codedSpecies.get(1).getId());
    }

    @AfterEach
    void restoreSpeciesCodes() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        codedSpecies.forEach(species -> ctx.execute("UPDATE species SET code_espece = ? WHERE id = ?",
                species.getCodeEspece(), species.getId()));
    }

    @Test
    void anglerTripExposesOriginAndSpeciesCodes() {
        CatchBean aCatch = new CatchBean();
        aCatch.id = "catch";
        aCatch.speciesId = Optional.of(codedSpecies.get(0).getId().toString());
        aCatch.techniqueId = techniqueId;
        aCatch.keep = false;
        aCatch.size = Optional.of(30);

        TripBean trip = new TripBean();
        trip.id = "angler-trip";
        trip.date = LocalDate.now().minusDays(1);
        trip.startedAt = "08:00";
        trip.finishedAt = "10:00";
        trip.waterEntityId = waterEntityId;
        trip.name = "Origine application";
        trip.type = TripType.Border;
        trip.mode = TripMode.Afterwards;
        trip.speciesIds = ImmutableSet.of(codedSpecies.get(0).getId(), codedSpecies.get(1).getId());
        trip.techniqueIds = ImmutableSet.of(techniqueId);
        trip.catchs = List.of(aCatch);

        String tripId = given()
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME, login("thimel@codelutin.com", "sispea"))
                .body(trip)
                .when().post("/api/v1/trips")
                .then().statusCode(201)
                .extract().path(trip.id);

        var row = DSL.using(dataSource, SQLDialect.POSTGRES).fetchOne(
                "SELECT origine_donnee, code_session, code_espece_capturee, code_espece_recherchee "
                        + "FROM catchs_pending_validation WHERE id_sortie = ?", UUID.fromString(tripId));
        Assertions.assertEquals("saisie_pecheur", row.get("origine_donnee", String.class));
        Assertions.assertNull(row.get("code_session", String.class));
        Assertions.assertEquals("ZZA", row.get("code_espece_capturee", String.class));
        Assertions.assertEquals("ZZA,ZZB", row.get("code_espece_recherchee", String.class));
    }
}
