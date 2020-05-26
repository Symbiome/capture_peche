package fr.inrae.fishola.rest.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static fr.inrae.fishola.rest.security.SecurityResource.encodeSampleBaseId;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityResourceUnitTests {

    @Test
    public void testEncodeSampleBaseId() {
        assertEquals("A", encodeSampleBaseId(0));
        assertEquals("B", encodeSampleBaseId(1));
        assertEquals("D", encodeSampleBaseId(3));
        assertEquals("Z", encodeSampleBaseId(25));
        assertEquals("AA", encodeSampleBaseId(26));
        assertEquals("AB", encodeSampleBaseId(27));
        assertEquals("AC", encodeSampleBaseId(28));
        assertEquals("AD", encodeSampleBaseId(29));
        assertEquals("AE", encodeSampleBaseId(30));
        assertEquals("AF", encodeSampleBaseId(31));
        assertEquals("AG", encodeSampleBaseId(32));
        assertEquals("AH", encodeSampleBaseId(33));
        assertEquals("AI", encodeSampleBaseId(34));
        assertEquals("AJ", encodeSampleBaseId(35));
        assertEquals("AK", encodeSampleBaseId(36));
        assertEquals("AL", encodeSampleBaseId(37));
        assertEquals("AM", encodeSampleBaseId(38));
        assertEquals("AN", encodeSampleBaseId(39));
        assertEquals("AO", encodeSampleBaseId(40));
        assertEquals("AP", encodeSampleBaseId(41));
        assertEquals("AQ", encodeSampleBaseId(42));
        assertEquals("AR", encodeSampleBaseId(43));
        assertEquals("AS", encodeSampleBaseId(44));
        assertEquals("AT", encodeSampleBaseId(45));
        assertEquals("AU", encodeSampleBaseId(46));
        assertEquals("AV", encodeSampleBaseId(47));
        assertEquals("AW", encodeSampleBaseId(48));
        assertEquals("AX", encodeSampleBaseId(49));
        assertEquals("AY", encodeSampleBaseId(50));
        assertEquals("AZ", encodeSampleBaseId(51));
        assertEquals("BA", encodeSampleBaseId(52));
        assertEquals("GH", encodeSampleBaseId(189));
        assertEquals("BALEC", encodeSampleBaseId(939772));
    }

    @Test
    public void testEncodeSampleBaseIdNoConflict() {
        Set<String> done = new HashSet<>();
        for (int i=0; i<1000000; i++) {
            String encoded = encodeSampleBaseId(i);
            Assertions.assertTrue(done.add(encoded));
        }
    }

}
