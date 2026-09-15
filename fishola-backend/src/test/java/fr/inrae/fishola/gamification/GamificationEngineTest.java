package fr.inrae.fishola.gamification;

/*-
 * #%L
 * Fishola :: Backend
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

import fr.inrae.fishola.database.CatchsDao;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.enums.DeviceType;
import fr.inrae.fishola.entities.enums.TripMode;
import fr.inrae.fishola.entities.enums.TripType;
import fr.inrae.fishola.entities.tables.pojos.Catch;
import fr.inrae.fishola.entities.tables.pojos.Trip;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Socle de gamification (#7, construit à l'occasion de #146) : vérifie de bout en bout
 * qu'une sortie + ses captures, une fois persistées, débloquent bien les badges dont la
 * règle est satisfaite -- sans déclencher ceux qui ne le sont pas -- et que la
 * réévaluation est idempotente (aucun doublon, mise à jour en place pour les badges
 * "record"). Auto-suffisant : provisionne son propre pêcheur pour ne pas être perturbé
 * par les sorties créées par d'autres suites de test dans le même conteneur.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GamificationEngineTest {

    @Inject
    GamificationEngine engine;

    @Inject
    GamificationDao dao;

    @Inject
    TripsDao tripsDao;

    @Inject
    CatchsDao catchsDao;

    @Inject
    AgroalDataSource dataSource;

    private final UUID userId = UUID.randomUUID();
    private UUID waterEntityId;
    private UUID techniqueCoupId;
    private UUID perceId;
    private UUID brochetId;
    private UUID truiteFarioId;
    private UUID sandreId;
    private UUID carpeCommuneId;
    private UUID ombreCommunId;

    @BeforeAll
    @Transactional
    void seed() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("INSERT INTO fishola_user (id, first_name, last_name, email, password, created_on, pseudo) "
                        + "VALUES (?, 'Gamification', 'Test', 'gamification-engine-test@fishola.test', 'x', now(), 'gamif-engine-test')",
                userId);
        waterEntityId = ctx.fetchOne("SELECT id FROM water_entity WHERE name = 'Annecy'").get("id", UUID.class);
        techniqueCoupId = ctx.fetchOne("SELECT id FROM technique WHERE name = 'Pêche au coup'").get("id", UUID.class);
        perceId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Perche'").get("id", UUID.class);
        brochetId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Brochet'").get("id", UUID.class);
        truiteFarioId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Truite fario'").get("id", UUID.class);
        sandreId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Sandre'").get("id", UUID.class);
        carpeCommuneId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Carpe commune'").get("id", UUID.class);
        ombreCommunId = ctx.fetchOne("SELECT id FROM species WHERE name = 'Ombre commun'").get("id", UUID.class);
    }

    @AfterAll
    @Transactional
    void cleanup() {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.execute("DELETE FROM gamification_badge_unlock WHERE fishola_user_id = ?", userId);
        ctx.execute("DELETE FROM catch WHERE trip_id IN (SELECT id FROM trip WHERE owner_id = ?)", userId);
        ctx.execute("DELETE FROM trip WHERE owner_id = ?", userId);
        ctx.execute("DELETE FROM fishola_user WHERE id = ?", userId);
    }

    private UUID createTrip(LocalDateTime begin, LocalDateTime end) {
        Trip trip = new Trip();
        trip.setCreatedOn(LocalDateTime.now());
        trip.setOwnerId(userId);
        trip.setMode(TripMode.Afterwards);
        trip.setName("Sortie de test #146");
        trip.setType(TripType.Border);
        trip.setWaterEntityId(waterEntityId);
        trip.setSource(DeviceType.web);
        trip.setBeginTimestamp(begin);
        trip.setEndTimestamp(end);
        return tripsDao.create(trip);
    }

    private void addCatch(UUID tripId, UUID speciesId, LocalDateTime catchTimestamp, Integer size) {
        Catch aCatch = new Catch();
        aCatch.setTripId(tripId);
        aCatch.setCreatedOn(LocalDateTime.now());
        aCatch.setCatchTimestamp(catchTimestamp);
        aCatch.setSpeciesId(speciesId);
        aCatch.setTechniqueId(techniqueCoupId);
        aCatch.setSize(size);
        aCatch.setKept(true);
        catchsDao.create(aCatch);
    }

    @Test
    @Transactional
    void unlocksMatchingBadgesAndSkipsUnmetOnes() {
        LocalDateTime begin = LocalDateTime.of(2026, 7, 1, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 1, 11, 0);
        UUID tripId = createTrip(begin, end);
        // Premier poisson à 08:05 : dans les 10 premières minutes -> PRISE_ECLAIR.
        addCatch(tripId, perceId, begin.plusMinutes(5), 25);
        addCatch(tripId, brochetId, begin.plusMinutes(20), 50);
        addCatch(tripId, truiteFarioId, begin.plusMinutes(30), 35);
        addCatch(tripId, sandreId, begin.plusMinutes(40), 10);
        addCatch(tripId, carpeCommuneId, begin.plusMinutes(50), 55);

        engine.evaluateForUser(userId);

        Set<UUID> unlockedBadgeIds = dao.listUnlocksForUser(userId).stream()
                .map(GamificationDao.UnlockRow::badgeId).collect(java.util.stream.Collectors.toSet());
        var badgesByCode = dao.listAllBadges().stream()
                .collect(java.util.stream.Collectors.toMap(GamificationDao.BadgeRow::code, b -> b));

        Assertions.assertTrue(unlockedBadgeIds.contains(badgesByCode.get("PRISE_ECLAIR").id()),
                "1ère capture à 5 min du début doit débloquer Prise éclair");
        Assertions.assertTrue(unlockedBadgeIds.contains(badgesByCode.get("COLLECTIONNEUR_1").id()),
                "5 espèces différentes doivent débloquer Collectionneur (palier 5)");
        Assertions.assertTrue(unlockedBadgeIds.contains(badgesByCode.get("RECORD_TAILLE").id()),
                "toute première capture avec taille doit être un nouveau record personnel");

        Assertions.assertFalse(unlockedBadgeIds.contains(badgesByCode.get("LEVE_TOT").id()),
                "aucune capture avant 7h -- Lève-tôt ne doit pas se débloquer");
        Assertions.assertFalse(unlockedBadgeIds.contains(badgesByCode.get("TECHNIQUE_COUP_1").id()),
                "une seule sortie -- le palier 5 sorties de la technique ne doit pas se débloquer");
        Assertions.assertFalse(unlockedBadgeIds.contains(badgesByCode.get("FIDELITE_1").id()),
                "une seule sortie -- le palier 10 sorties ne doit pas se débloquer");

        // 5 espèces toutes "record" simultanément (1ère évaluation) : la stratégie retient
        // celle à la plus grande valeur, ici Carpe commune (55 cm).
        GamificationDao.UnlockRow recordUnlock = dao.findUnlock(badgesByCode.get("RECORD_TAILLE").id(), userId, (short) 0)
                .orElseThrow();
        Assertions.assertEquals("Carpe commune", recordUnlock.context().get("species").asText());
        Assertions.assertEquals(55, recordUnlock.context().get("sizeCm").asInt());
    }

    @Test
    @Transactional
    void reevaluationIsIdempotentAndUpdatesRecordsInPlace() {
        LocalDateTime begin = LocalDateTime.of(2026, 7, 2, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 2, 10, 0);
        UUID tripId = createTrip(begin, end);
        // Ombre commun, dédié à ce test (pas touché par unlocksMatchingBadgesAndSkipsUnmetOnes)
        // pour que ces assertions ne dépendent pas de l'ordre d'exécution des méthodes.
        addCatch(tripId, ombreCommunId, begin.plusMinutes(15), 22);

        engine.evaluateForUser(userId);
        engine.evaluateForUser(userId);

        UUID recordBadgeId = dao.listAllBadges().stream()
                .filter(b -> "RECORD_TAILLE".equals(b.code())).findFirst().orElseThrow().id();
        long rowsForBadge = countUnlockRows(recordBadgeId);
        Assertions.assertEquals(1, rowsForBadge, "une réévaluation sans nouveau record ne doit pas dupliquer la ligne");

        // Nouveau record de taille pour l'Ombre commun (30 > 22) sur une nouvelle sortie :
        // le badge existant doit être mis à jour, pas doublonné.
        UUID tripId2 = createTrip(begin.plusDays(1), end.plusDays(1));
        addCatch(tripId2, ombreCommunId, begin.plusDays(1).plusMinutes(15), 30);
        engine.evaluateForUser(userId);

        Assertions.assertEquals(1, countUnlockRows(recordBadgeId), "la mise à jour d'un record ne doit pas créer de 2e ligne");
        Optional<Integer> newSize = dao.findUnlock(recordBadgeId, userId, (short) 0)
                .map(u -> u.context().get("sizeCm").asInt());
        Assertions.assertEquals(Optional.of(30), newSize, "le contexte du badge record doit refléter la nouvelle meilleure taille");
    }

    private long countUnlockRows(UUID badgeId) {
        var ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchOne("SELECT count(*) AS n FROM gamification_badge_unlock WHERE badge_id = ? AND fishola_user_id = ?",
                badgeId, userId).get("n", Long.class);
    }
}
