package fr.inra.fishola.rest.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.HashSet;
import java.util.Set;

class SecurityResourceTest {

    @Test
    public void testEncodeSampleBaseId() {
        Assertions.assertEquals("A", SecurityResource.encodeSampleBaseId(0));
        Assertions.assertEquals("B", SecurityResource.encodeSampleBaseId(1));
        Assertions.assertEquals("D", SecurityResource.encodeSampleBaseId(3));
        Assertions.assertEquals("Z", SecurityResource.encodeSampleBaseId(25));
        Assertions.assertEquals("AA", SecurityResource.encodeSampleBaseId(26));
        Assertions.assertEquals("AB", SecurityResource.encodeSampleBaseId(27));
        Assertions.assertEquals("AC", SecurityResource.encodeSampleBaseId(28));
        Assertions.assertEquals("AD", SecurityResource.encodeSampleBaseId(29));
        Assertions.assertEquals("AE", SecurityResource.encodeSampleBaseId(30));
        Assertions.assertEquals("AF", SecurityResource.encodeSampleBaseId(31));
        Assertions.assertEquals("AG", SecurityResource.encodeSampleBaseId(32));
        Assertions.assertEquals("AH", SecurityResource.encodeSampleBaseId(33));
        Assertions.assertEquals("AI", SecurityResource.encodeSampleBaseId(34));
        Assertions.assertEquals("AJ", SecurityResource.encodeSampleBaseId(35));
        Assertions.assertEquals("AK", SecurityResource.encodeSampleBaseId(36));
        Assertions.assertEquals("AL", SecurityResource.encodeSampleBaseId(37));
        Assertions.assertEquals("AM", SecurityResource.encodeSampleBaseId(38));
        Assertions.assertEquals("AN", SecurityResource.encodeSampleBaseId(39));
        Assertions.assertEquals("AO", SecurityResource.encodeSampleBaseId(40));
        Assertions.assertEquals("AP", SecurityResource.encodeSampleBaseId(41));
        Assertions.assertEquals("AQ", SecurityResource.encodeSampleBaseId(42));
        Assertions.assertEquals("AR", SecurityResource.encodeSampleBaseId(43));
        Assertions.assertEquals("AS", SecurityResource.encodeSampleBaseId(44));
        Assertions.assertEquals("AT", SecurityResource.encodeSampleBaseId(45));
        Assertions.assertEquals("AU", SecurityResource.encodeSampleBaseId(46));
        Assertions.assertEquals("AV", SecurityResource.encodeSampleBaseId(47));
        Assertions.assertEquals("AW", SecurityResource.encodeSampleBaseId(48));
        Assertions.assertEquals("AX", SecurityResource.encodeSampleBaseId(49));
        Assertions.assertEquals("AY", SecurityResource.encodeSampleBaseId(50));
        Assertions.assertEquals("AZ", SecurityResource.encodeSampleBaseId(51));
        Assertions.assertEquals("BA", SecurityResource.encodeSampleBaseId(52));
        Assertions.assertEquals("GH", SecurityResource.encodeSampleBaseId(189));
        Assertions.assertEquals("BALEC", SecurityResource.encodeSampleBaseId(939772));
    }

    @Test
    public void testEncodeSampleBaseIdNoConflict() {
        Set<String> done = new HashSet<>();
        for (int i=0; i<1000000; i++) {
            String encoded = SecurityResource.encodeSampleBaseId(i);
            Assert.assertTrue(done.add(encoded));
        }

    }

}