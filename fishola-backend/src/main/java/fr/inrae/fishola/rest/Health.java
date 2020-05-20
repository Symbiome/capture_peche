package fr.inrae.fishola.rest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableHealth.class)
public interface Health {

    String version();

    String gitRevision();

    String buildDate();

    String encoding();

    String jvmName();

    String javaVersion();

    String memoryAllocated();

    String memoryUsed();

    String memoryFree();

    String memoryMax();

    double loadAverage();

    int availableProcessors();

    String runningSince();

    String uptime();

    long duration();

    String currentDate();

    String currentTimeZone();

}
