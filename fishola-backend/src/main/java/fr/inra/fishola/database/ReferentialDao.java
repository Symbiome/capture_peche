package fr.inra.fishola.database;

import fr.inra.fishola.entities.tables.records.LakeRecord;
import fr.inra.fishola.entities.tables.records.MethodRecord;
import fr.inra.fishola.entities.tables.records.SpeciesRecord;
import fr.inra.fishola.entities.tables.records.WeatherRecord;
import org.jooq.Result;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static fr.inra.fishola.entities.Tables.LAKE;
import static fr.inra.fishola.entities.Tables.METHOD;
import static fr.inra.fishola.entities.Tables.SPECIES;
import static fr.inra.fishola.entities.Tables.WEATHER;

@Singleton
public class ReferentialDao extends AbstractFisholaDao {

    public Map<UUID, String> listLakes() {

        return run(context -> {
            Result<LakeRecord> records = context.selectFrom(LAKE)
                    .orderBy(LAKE.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (LakeRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        });

    }

    public Map<UUID, String> listWeathers() {

        return run(context -> {
            Result<WeatherRecord> records = context.selectFrom(WEATHER)
                    .orderBy(WEATHER.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (WeatherRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        });

    }

    public Map<UUID, String> listBuiltInMethods() {

        return run(context -> {
            Result<MethodRecord> records = context.selectFrom(METHOD)
                    .where(METHOD.BUILT_IN.equal(true))
                    .orderBy(METHOD.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (MethodRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        });
    }

    public Map<UUID, String> listBuiltInSpecies() {

        return run(context -> {
            Result<SpeciesRecord> records = context.selectFrom(SPECIES)
                    .where(SPECIES.BUILT_IN.equal(true))
                    .orderBy(SPECIES.NAME)
                    .fetch();

            Map<UUID, String> result = new HashMap<>();
            for (SpeciesRecord record : records) {
                result.put(record.getId(), record.getName());
            }

            return result;
        });
    }

}
