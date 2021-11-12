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

import fr.inrae.fishola.FisholaConfiguration;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

@Path("/api/v1/status")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    protected static final List<String> READABLE_SIZE_UNITS = Arrays.asList("B", "KB", "MB", "GB", "TB", "PB");
    protected static final LocalDateTime RUNNING_SINCE = LocalDateTime.now();
    protected static final double ONE_BYTE_AS_DOUBLE = 1024d;

    @Inject
    protected FisholaConfiguration config;

    public static String asReadableSize(Long bytes) {
        Iterator<String> iterator = READABLE_SIZE_UNITS.iterator();
        double bytesAsDouble = bytes.doubleValue();
        String unit = iterator.next();
        while (bytesAsDouble > ONE_BYTE_AS_DOUBLE) {
            bytesAsDouble /= ONE_BYTE_AS_DOUBLE;
            unit = iterator.next();
        }
        String result = String.format("%.2f%s", bytesAsDouble, unit);
        return result;
    }

    public static String formatDuration(LocalDateTime from, LocalDateTime to) {
        long s = ChronoUnit.SECONDS.between(from, to);
        long m = ChronoUnit.MINUTES.between(from, to);
        long h = ChronoUnit.HOURS.between(from, to);
        long d = ChronoUnit.DAYS.between(from, to);
        String result = String.format("%ds", (s % 60));
        if (m > 0) {
            result = String.format("%dm", (m % 60)) + result;
        }
        if (h > 0) {
            result = String.format("%dh", (h % 24)) + result;
        }
        if (d > 0) {
            result = String.format("%dd", d) + result;
        }
        return result;
    }

    protected void appendMemoryValues(ImmutableHealth.Builder builder) {
        // Mémoire : Données brutes
        Runtime runtime = Runtime.getRuntime();
        long freeMemoryOnAllocated = runtime.freeMemory();            // Mémoire libre (par rapport à la mémoire allouée)
        long totalMemory = runtime.totalMemory();                     // Mémoire allouée
        long maxMemory = runtime.maxMemory();                         // Mémoire totale (max)

        // Mémoire : Données déduites
        long usedMemory = totalMemory - freeMemoryOnAllocated;        // Mémoire utilisée (allouée - libre)
        double usedPercent = ((double) usedMemory / maxMemory) * 100d;// Mémoire utilisée en pourcentage du max
        long freeMemory = maxMemory - usedMemory;                     // Mémoire libre (par rapport au max)
        double freePercent = 100d - usedPercent;                      // Mémoire libre en pourcentage du max

        builder.memoryAllocated(asReadableSize(totalMemory));
        builder.memoryUsed(String.format("%s (%.2f%s)", asReadableSize(usedMemory), usedPercent, "%"));
        builder.memoryFree(String.format("%s (%.2f%s)", asReadableSize(freeMemory), freePercent, "%"));
        builder.memoryMax(asReadableSize(maxMemory));
    }

    public Health compute() {

        long statusStart = System.currentTimeMillis();

        ImmutableHealth.Builder builder = ImmutableHealth.builder();

        builder.version(config.version());
        builder.gitRevision(config.gitRevision());
        builder.buildDate(config.buildDate());

        builder.currentDate(new Date().toString());
        builder.currentTimeZone(TimeZone.getDefault().toString());

        builder.encoding(System.getProperty("file.encoding"));

        String jvmName = System.getProperty("java.vm.name");
        builder.jvmName(jvmName);

        String javaVersion = System.getProperty("java.version");
        builder.javaVersion(javaVersion);

        appendMemoryValues(builder);

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        builder.availableProcessors(availableProcessors);

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double systemLoadAverage = os.getSystemLoadAverage();
        builder.loadAverage(systemLoadAverage);

        LocalDateTime startupTime = RUNNING_SINCE;
        String runningSince = startupTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        builder.runningSince(runningSince);
        builder.uptime(formatDuration(startupTime, LocalDateTime.now()));

        long statusEnd = System.currentTimeMillis();

        builder.duration(statusEnd - statusStart);

        Health result = builder.build();
        return result;
    }

    @GET
    public Health hello() {
        Health result = compute();
        return result;
    }

}
