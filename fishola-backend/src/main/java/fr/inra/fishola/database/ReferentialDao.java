package fr.inra.fishola.database;

import fr.inra.fishola.entities.Tables;
import fr.inra.fishola.entities.tables.records.LakeRecord;
import fr.inra.fishola.entities.tables.records.MethodRecord;
import fr.inra.fishola.entities.tables.records.SpeciesRecord;
import fr.inra.fishola.entities.tables.records.WeatherRecord;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class ReferentialDao extends AbstractFisholaDao {

    public Map<UUID, String> listLakes() {

        try (Connection conn = newConnection()) {
            DSLContext context = DSL.using(conn, SQLDialect.POSTGRES);
            Result<LakeRecord> records = context.selectFrom(Tables.LAKE)
                    .orderBy(Tables.LAKE.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (LakeRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to list lakes", e);
        }

    }

    public Map<UUID, String> listWeathers() {

        try (Connection conn = newConnection()) {
            DSLContext context = DSL.using(conn, SQLDialect.POSTGRES);
            Result<WeatherRecord> records = context.selectFrom(Tables.WEATHER)
                    .orderBy(Tables.WEATHER.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (WeatherRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to list lakes", e);
        }

    }

    public Map<UUID, String> listBuiltInMethods() {

        try (Connection conn = newConnection()) {
            DSLContext context = DSL.using(conn, SQLDialect.POSTGRES);
            Result<MethodRecord> records = context.selectFrom(Tables.METHOD)
                    .where(Tables.METHOD.BUILT_IN.equal(true))
                    .orderBy(Tables.METHOD.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (MethodRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to list lakes", e);
        }
    }

    public Map<UUID, String> listBuiltInSpecies() {

        try (Connection conn = newConnection()) {
            DSLContext context = DSL.using(conn, SQLDialect.POSTGRES);
            Result<SpeciesRecord> records = context.selectFrom(Tables.SPECIES)
                    .where(Tables.SPECIES.BUILT_IN.equal(true))
                    .orderBy(Tables.SPECIES.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (SpeciesRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to list lakes", e);
        }
    }

}
