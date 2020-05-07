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
        Assertions.assertTrue(parsed.isPresent());
        Assertions.assertEquals(6, parsed.get().getDayOfMonth());
        Assertions.assertEquals(5, parsed.get().getMonthValue());
        Assertions.assertEquals(2020, parsed.get().getYear());
        Assertions.assertEquals(15, parsed.get().getHour());
        Assertions.assertEquals(49, parsed.get().getMinute());
    }

}
