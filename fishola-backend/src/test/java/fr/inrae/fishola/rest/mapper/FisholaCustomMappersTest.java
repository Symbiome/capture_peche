package fr.inrae.fishola.rest.mapper;

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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

class FisholaCustomMappersTest {

    @Test
    void readIso8601AtZone() {
        Optional<LocalDateTime> parsed = FisholaCustomMappers.readIso8601AtZone("2020-05-06T15:49:02.640Z", ZoneId.of("Europe/Paris"));
        Assertions.assertNotNull(parsed);
        Assertions.assertTrue(parsed.isPresent());
        Assertions.assertEquals(6, parsed.get().getDayOfMonth());
        Assertions.assertEquals(5, parsed.get().getMonthValue());
        Assertions.assertEquals(2020, parsed.get().getYear());
        Assertions.assertEquals(17, parsed.get().getHour());
        Assertions.assertEquals(49, parsed.get().getMinute());
    }

}
