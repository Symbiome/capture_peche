package fr.inrae.fishola.database;

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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReferentialDaoTest {

    @Test
    void testNormalizeSpeciesName() {
        Assertions.assertNull(ReferentialDao.normalizeSpeciesName("    "));
        Assertions.assertEquals("Saint pierre", ReferentialDao.normalizeSpeciesName(" Saint    Pierre   "));
        Assertions.assertEquals("Saint-Pierre", ReferentialDao.normalizeSpeciesName(" Saint - pierre   "));
        Assertions.assertEquals("Saint-Pièrre !", ReferentialDao.normalizeSpeciesName(" Saint - pièrre   !  "));

        Assertions.assertEquals("Omble chevalier", ReferentialDao.normalizeSpeciesName("Omble chevalier"));
        Assertions.assertEquals("Corégone", ReferentialDao.normalizeSpeciesName("Corégone"));
        Assertions.assertEquals("Brochet", ReferentialDao.normalizeSpeciesName("Brochet"));
        Assertions.assertEquals("Perche", ReferentialDao.normalizeSpeciesName("Perche"));
        Assertions.assertEquals("Truite", ReferentialDao.normalizeSpeciesName("Truite"));
    }

    @Test
    void testNormalizeSpeciesExportAs() {
        Assertions.assertEquals("saint-pierre", ReferentialDao.normalizeSpeciesExportAs(" Saint - pièrre   !  "));


        Assertions.assertEquals("omble chevalier", ReferentialDao.normalizeSpeciesExportAs("Omble chevalier"));
        Assertions.assertEquals("coregone", ReferentialDao.normalizeSpeciesExportAs("Corégone"));
        Assertions.assertEquals("brochet", ReferentialDao.normalizeSpeciesExportAs("Brochet"));
        Assertions.assertEquals("perche", ReferentialDao.normalizeSpeciesExportAs("Perche"));
        Assertions.assertEquals("truite", ReferentialDao.normalizeSpeciesExportAs("Truite"));
    }

}
