package fr.inrae.fishola.rest;

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

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class StatusResourceTest {

    @Test
    void testStatusEndpoint() {
        // {"version":"%(project.version)","gitRevision":"%(git.commit.id.describe)","buildDate":"%(maven.build.timestamp)","encoding":"UTF-8","jvmName":"OpenJDK 64-Bit Server VM","javaVersion":"17.0.1","memoryAllocated":"280,00MB","memoryUsed":"138,56MB (3,48%)","memoryFree":"3,75GB (96,52%)","memoryMax":"3,89GB","loadAverage":1.8,"availableProcessors":16,"runningSince":"04/11/2021 10:12:17","uptime":"3s","duration":6,"currentDate":"Thu Nov 04 10:12:20 CET 2021","currentTimeZone":"sun.util.calendar.ZoneInfo[id=\"Europe/Paris\",offset=3600000,dstSavings=3600000,useDaylight=true,transitions=184,lastRule=java.util.SimpleTimeZone[id=Europe/Paris,offset=3600000,dstSavings=3600000,useDaylight=true,startYear=0,startMode=2,startMonth=2,startDay=-1,startDayOfWeek=1,startTime=3600000,startTimeMode=2,endMode=2,endMonth=9,endDay=-1,endDayOfWeek=1,endTime=3600000,endTimeMode=2]]"}
        given()
                .when()
                    .get("/api/v1/status")
                .then()
                    .statusCode(200);
    }

}
