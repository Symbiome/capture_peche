package fr.inra.fishola.database;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReferentialDaoTest {

    @Test
    public void testNormalizeSpeciesName() {
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
    public void testNormalizeSpeciesExportAs() {
        Assertions.assertEquals("saint-pierre", ReferentialDao.normalizeSpeciesExportAs(" Saint - pièrre   !  "));


        Assertions.assertEquals("omble chevalier", ReferentialDao.normalizeSpeciesExportAs("Omble chevalier"));
        Assertions.assertEquals("coregone", ReferentialDao.normalizeSpeciesExportAs("Corégone"));
        Assertions.assertEquals("brochet", ReferentialDao.normalizeSpeciesExportAs("Brochet"));
        Assertions.assertEquals("perche", ReferentialDao.normalizeSpeciesExportAs("Perche"));
        Assertions.assertEquals("truite", ReferentialDao.normalizeSpeciesExportAs("Truite"));
    }

}
