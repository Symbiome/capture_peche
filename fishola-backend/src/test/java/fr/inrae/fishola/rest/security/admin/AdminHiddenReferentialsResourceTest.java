package fr.inrae.fishola.rest.security.admin;

/*-
 * #%L
 * Carnet de pêche :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

import fr.inrae.fishola.rest.AbstractCarnetResource;
import fr.inrae.fishola.rest.JwtHelper;
import fr.inrae.fishola.rest.imports.carnet.CarnetVolontaireSchema;
import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * Référentiels personnalisés par compte staff (#239) : éléments masqués propres à chaque
 * compte, exclus des listes de saisie ({@code forCurrentAdmin=true}) mais toujours servis
 * par les listings complets, nouvel élément national affiché par défaut, archivage
 * national prioritaire, import de fichiers non filtré.
 *
 * <p>Auto-suffisant : provisionne deux opérateurs, deux espèces et une technique dédiées.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminHiddenReferentialsResourceTest {

    private static final String URI = "/api/v1/admin/hidden-referentials/";
    private static final String SPECIES_URI = "/api/v1/referential/species";
    private static final String TECHNIQUES_URI = "/api/v1/referential/techniques";
    private static final String CARNET_IMPORT_URI = "/api/v1/admin/imports/carnet-volontaire";
    private static final String SPECIES_NAME = "Zzmasquage espèce";
    private static final String TECHNIQUE_NAME = "Zzmasquage technique";

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AgroalDataSource dataSource;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID otherOperatorId = UUID.randomUUID();
    private final UUID speciesId = UUID.randomUUID();
    private final UUID archivedSpeciesId = UUID.randomUUID();
    private final UUID newSpeciesId = UUID.randomUUID();
    private final UUID techniqueId = UUID.randomUUID();
    private String operatorToken;
    private String otherOperatorToken;
    private String waterEntityName;

    @BeforeAll
    @Transactional
    void seed() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var annecy = ctx.fetchOne("SELECT name, department FROM water_entity WHERE name = 'Annecy' LIMIT 1");
        waterEntityName = annecy.get("name", String.class);
        insertOperator(ctx, operatorId, "hidden-ref-op@carnet-peche.test", annecy.get("department", String.class));
        insertOperator(ctx, otherOperatorId, "hidden-ref-op-2@carnet-peche.test", annecy.get("department", String.class));
        insertSpecies(ctx, speciesId, SPECIES_NAME, false);
        insertSpecies(ctx, archivedSpeciesId, "Zzmasquage espèce archivée", true);
        ctx.execute("INSERT INTO technique (id, name, export_as, built_in) VALUES (?, ?, ?, true)",
                techniqueId, TECHNIQUE_NAME, "ZzmasquageTechnique");

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        try {
            operatorToken = jwtHelper.createAdminToken(operatorId);
            otherOperatorToken = jwtHelper.createAdminToken(otherOperatorId);
        } finally {
            requestContext.deactivate();
        }
    }

    private static void insertOperator(DSLContext ctx, UUID id, String email, String department) {
        ctx.execute("INSERT INTO fishola_admin (id, email, password, created_on, can_create_admin, is_national_admin, "
                + "is_operator) VALUES (?, ?, ?, now(), false, false, true)", id, email, "x");
        ctx.execute("INSERT INTO fishola_admin_departments (fishola_admin_id, department_code) VALUES (?, ?)",
                id, department);
    }

    private static void insertSpecies(DSLContext ctx, UUID id, String name, boolean archived) {
        ctx.execute("INSERT INTO species (id, name, export_as, built_in, mandatory_size, archived) "
                + "VALUES (?, ?, ?, true, false, ?)", id, name, name.replace(' ', '_'), archived);
    }

    @AfterEach
    @Transactional
    void showEverything() {
        DSL.using(dataSource, SQLDialect.POSTGRES).execute(
                "DELETE FROM fishola_admin_hidden_referential WHERE fishola_admin_id IN (?, ?)",
                operatorId, otherOperatorId);
    }

    @AfterAll
    @Transactional
    void cleanup() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE name LIKE '%HIDDEN-REF-TEST%')");
        ctx.execute("DELETE FROM trip WHERE name LIKE '%HIDDEN-REF-TEST%'");
        ctx.execute("DELETE FROM import_row_error WHERE import_id IN "
                + "(SELECT id FROM import_job WHERE file_name LIKE 'hidden-ref-test%')");
        ctx.execute("DELETE FROM import_job WHERE file_name LIKE 'hidden-ref-test%'");
        ctx.execute("DELETE FROM species WHERE id IN (?, ?, ?)", speciesId, archivedSpeciesId, newSpeciesId);
        ctx.execute("DELETE FROM technique WHERE id = ?", techniqueId);
        ctx.execute("DELETE FROM audit_log WHERE actor_id IN (?, ?)", operatorId, otherOperatorId);
        ctx.execute("DELETE FROM fishola_admin WHERE id IN (?, ?)", operatorId, otherOperatorId);
    }

    private ValidatableResponse hide(String token, String type, UUID... itemIds) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .contentType(ContentType.JSON)
                .body(List.of(itemIds))
                .when().put(URI + type)
                .then();
    }

    private ValidatableResponse get(String token, String uri) {
        return given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, token)
                .when().get(uri)
                .then().statusCode(200);
    }

    @Test
    void hiddenItemsAreExcludedFromTheirOwnerEntryListsOnly() {
        hide(operatorToken, "species", speciesId).statusCode(204);
        hide(operatorToken, "technique", techniqueId).statusCode(204);

        get(operatorToken, URI + "species").body("", equalTo(List.of(speciesId.toString())));
        get(operatorToken, URI + "technique").body("", equalTo(List.of(techniqueId.toString())));
        get(operatorToken, SPECIES_URI + "?forCurrentAdmin=true").body("id", not(hasItem(speciesId.toString())));
        get(operatorToken, TECHNIQUES_URI + "?forCurrentAdmin=true")
                .body("id", not(hasItem(techniqueId.toString())));

        get(otherOperatorToken, URI + "species").body("", empty());
        get(otherOperatorToken, SPECIES_URI + "?forCurrentAdmin=true").body("id", hasItem(speciesId.toString()));
        get(otherOperatorToken, TECHNIQUES_URI + "?forCurrentAdmin=true")
                .body("id", hasItem(techniqueId.toString()));
    }

    @Test
    void displayChoiceIsAudited() {
        hide(otherOperatorToken, "species", speciesId).statusCode(204);

        int audited = DSL.using(dataSource, SQLDialect.POSTGRES)
                .fetchOne("SELECT count(*) FROM audit_log WHERE action = 'hiddenReferential.update' AND actor_id = ?",
                        otherOperatorId)
                .get(0, Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, audited, "le choix d'affichage doit être journalisé");
    }

    @Test
    void hiddenItemsStayReadableInFullListings() {
        hide(operatorToken, "species", speciesId).statusCode(204);
        hide(operatorToken, "technique", techniqueId).statusCode(204);

        get(operatorToken, SPECIES_URI).body("id", hasItem(speciesId.toString()));
        get(operatorToken, "/api/v1/referential/raw-species").body("id", hasItem(speciesId.toString()));
        get(operatorToken, TECHNIQUES_URI).body("id", hasItem(techniqueId.toString()));
    }

    @Test
    void emptyListShowsEverythingAgain() {
        hide(operatorToken, "species", speciesId).statusCode(204);
        hide(operatorToken, "species").statusCode(204);

        get(operatorToken, URI + "species").body("", empty());
        get(operatorToken, SPECIES_URI + "?forCurrentAdmin=true").body("id", hasItem(speciesId.toString()));
    }

    @Test
    void newNationalItemIsShownByDefault() {
        hide(operatorToken, "species", speciesId).statusCode(204);
        insertSpecies(DSL.using(dataSource, SQLDialect.POSTGRES), newSpeciesId, "Zzmasquage espèce nouvelle", false);

        get(operatorToken, SPECIES_URI + "?forCurrentAdmin=true").body("id", hasItem(newSpeciesId.toString()));
    }

    @Test
    void nationalArchivingPrevailsOverDisplayChoice() {
        get(operatorToken, URI + "species").body("", empty());
        get(operatorToken, SPECIES_URI + "?forCurrentAdmin=true")
                .body("id", not(hasItem(archivedSpeciesId.toString())));
    }

    @Test
    void importAcceptsHiddenItems() {
        hide(operatorToken, "species", speciesId).statusCode(204);
        hide(operatorToken, "technique", techniqueId).statusCode(204);
        String row = String.join(";",
                "HIDDEN-REF-TEST-1", waterEntityName, "", "01/07/2026", "bord statique", "08:00", "11:30",
                TECHNIQUE_NAME, "", "1", "", "Aucune", "", "non",
                SPECIES_NAME, "", "", "", "", "25", "210", "1", "oui", "", "", "non", "");
        String csv = String.join(";", CarnetVolontaireSchema.LEGACY_HEADER) + "\n" + row + "\n";

        given().cookie(AbstractCarnetResource.ADMIN_AUTHENTICATION_COOKIE_NAME, operatorToken)
                .contentType("application/octet-stream")
                .body(csv.getBytes(StandardCharsets.UTF_8))
                .when().post(CARNET_IMPORT_URI + "?filename=hidden-ref-test.csv&mode=partial")
                .then().statusCode(200)
                .body("status", equalTo("DONE"))
                .body("inserted", equalTo(1))
                .body("rejected", equalTo(0));
    }

    @Test
    void unknownItemIsRejected() {
        hide(operatorToken, "species", UUID.randomUUID()).statusCode(400);
        hide(operatorToken, "species", techniqueId).statusCode(400);
        get(operatorToken, URI + "species").body("", empty());
    }

    @Test
    void nonHideableReferentialIsNotFound() {
        hide(operatorToken, "weather", speciesId).statusCode(404);
    }

    @Test
    void anonymousCallIsRefused() {
        given().when().get(URI + "species").then().statusCode(401);
        given().when().get(SPECIES_URI + "?forCurrentAdmin=true").then().statusCode(401);
    }
}
