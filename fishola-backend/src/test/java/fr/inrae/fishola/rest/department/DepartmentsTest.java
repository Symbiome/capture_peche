package fr.inrae.fishola.rest.department;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2026 INRAE - UMR CARRTEL
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

import java.util.Optional;

/** Département d'un code postal (#188). */
class DepartmentsTest {

    @Test
    void metropolitanPostalCodeUsesFirstTwoDigits() {
        Assertions.assertEquals(Optional.of("74"), Departments.fromPostalCode("74000"));
        Assertions.assertEquals(Optional.of("01"), Departments.fromPostalCode("01000"));
    }

    @Test
    void corsicanPostalCodeIsSplitBetween2AAnd2B() {
        Assertions.assertEquals(Optional.of("2A"), Departments.fromPostalCode("20000"));
        Assertions.assertEquals(Optional.of("2A"), Departments.fromPostalCode("20190"));
        Assertions.assertEquals(Optional.of("2B"), Departments.fromPostalCode("20200"));
        Assertions.assertEquals(Optional.of("2B"), Departments.fromPostalCode("20600"));
    }

    @Test
    void overseasPostalCodeUsesFirstThreeDigits() {
        Assertions.assertEquals(Optional.of("974"), Departments.fromPostalCode("97400"));
    }

    @Test
    void missingOrNonDepartmentalPostalCodeHasNoDepartment() {
        Assertions.assertEquals(Optional.empty(), Departments.fromPostalCode(null));
        Assertions.assertEquals(Optional.empty(), Departments.fromPostalCode("7400"));
        Assertions.assertEquals(Optional.empty(), Departments.fromPostalCode("98000"));
        Assertions.assertEquals(Optional.empty(), Departments.fromPostalCode("97800"));
    }
}
