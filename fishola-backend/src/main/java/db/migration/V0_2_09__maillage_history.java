package db.migration;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2023 INRAE - UMR CARRTEL
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V0_2_09__maillage_history extends BaseJavaMigration {

    private static final String MAILLAGE_CSV = "Tailles minimales de capture (cm);Lac d'Annecy;Lac du Bourget;Léman ;Lac d'Aiguebelette\n" +
            "Truite;50;50;35;30\n" +
            "Omble chevalier;30;30;30;30\n" +
            "Corégone;37;35;30;35\n" +
            "Brochet;50;50;45;60\n" +
            "Perche;;;15;\n" +
            "Sandre;;;;50\n" +
            "Black-Bass;;;;40\n";

    @Override
    public void migrate(Context context) throws Exception {
        Connection con = context.getConnection();
        con.beginRequest();

        try (PreparedStatement getLakeIdSt = con.prepareStatement("SELECT id from lake where name = ?")) {
        try (PreparedStatement getSpecieIdSt = con.prepareStatement("SELECT id from species where lower(name) = ?")) {
        try (PreparedStatement deleteSampleSizeSt = con.prepareStatement("DELETE from authorized_sample where species_id = ?::uuid and lake_id = ?::uuid")) {
        try (PreparedStatement insertSampleSizeSt = con.prepareStatement("INSERT into authorized_sample (species_id, lake_id, min_size) VALUES (?::uuid, ?::uuid, ?::integer)")) {
        try (PreparedStatement updateMailleeSt = con.prepareStatement("UPDATE catch set maillee = 'MAILLEE' where species_id = ?::uuid and trip_id in (select id from trip where lake_id = ?::uuid) and size >= ?")) {
        try (PreparedStatement updateNonMailleeSt = con.prepareStatement("UPDATE catch set maillee = 'NON_MAILLEE' where species_id = ?::uuid and trip_id in (select id from trip where lake_id = ?::uuid) and size < ?")) {
            String[] speciesSizesPerLake = MAILLAGE_CSV.split("\n");
            String[] lakes = speciesSizesPerLake[0].split(";");
            for (int i = 1; i < speciesSizesPerLake.length; i++) {
                String[] specieSizes = speciesSizesPerLake[i].split(";");
                getSpecieIdSt.setString(1, specieSizes[0].toLowerCase());
                try (ResultSet specieIdRs = getSpecieIdSt.executeQuery()) {
                    if (specieIdRs.next()) {
                        String specieId = specieIdRs.getString(1);
                        for (int j = 1; j < specieSizes.length; j++) {
                            String lakeName = lakes[j].trim();
                            getLakeIdSt.setString(1,  lakeName);
                            try (ResultSet getLakeIdRs = getLakeIdSt.executeQuery()) {
                                if (getLakeIdRs.next()) {
                                    String lakeId = getLakeIdRs.getString(1);
                                    String size = specieSizes[j];
                                    if (!size.isEmpty() && Integer.parseInt(size) > 0) {
                                        deleteSampleSizeSt.setString(1, specieId);
                                        deleteSampleSizeSt.setString(2, lakeId);
                                        deleteSampleSizeSt.execute();
                                        insertSampleSizeSt.setString(1, specieId);
                                        insertSampleSizeSt.setString(2, lakeId);
                                        insertSampleSizeSt.setInt(3, Integer.parseInt(size));
                                        insertSampleSizeSt.execute();

                                        updateMailleeSt.setString(1, specieId);
                                        updateMailleeSt.setString(2, lakeId);
                                        updateMailleeSt.setInt(3, Integer.parseInt(size));
                                        updateMailleeSt.execute();

                                        updateNonMailleeSt.setString(1, specieId);
                                        updateNonMailleeSt.setString(2, lakeId);
                                        updateNonMailleeSt.setInt(3, Integer.parseInt(size));
                                        updateNonMailleeSt.execute();
                                    }
                                } else {
                                    throw new RuntimeException("Invalid lake name : " + lakeName);
                                }
                            }
                        }
                    } else {
                        throw new RuntimeException("Invalid specie name :" + specieSizes[0]);
                    }
                }
            }
        }
        }
        }
        }
        }
        }
        con.commit();
    }
}
