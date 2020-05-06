package fr.inra.fishola.rest.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

class FisholaCustomMappersTest {

    @Test
    public void testReadIso8601Date() {
        Optional<LocalDateTime> parsed = FisholaCustomMappers.readIso8601Date("2020-05-06T15:49:02.640Z");
        Assertions.assertNotNull(parsed);
        // FIXME AThimel 06/05/2020 Ne devrait pas échouer
        Assertions.assertTrue(parsed.isPresent());
    }

}
