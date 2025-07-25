package fr.inrae.fishola.rest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.rest.about.KeyFigures;
import fr.inrae.fishola.rest.dashboard.EvolutionMetricsForLake;
import fr.inrae.fishola.rest.dashboard.GlobalDashboard;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class FisholaCache {

    public final Cache<String, GlobalDashboard> globalDashboard;

    public final Cache<String, EvolutionMetricsForLake> globalEvolution;

    public final Cache<String, EvolutionMetricsForLake> personalEvolution;


    public final Cache<String, KeyFigures> keyFigures;

    @Inject
    public FisholaCache(FisholaConfiguration config) {
         this.globalDashboard = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(config.globalDashboardTimeoutMinutes()))
                .maximumSize(config.globalDashboardCacheSize())
                .build();

        this.globalEvolution = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(config.globalDashboardTimeoutMinutes()))
                .maximumSize(config.keyFiguresTimeoutHours())
                .build();

        this.personalEvolution = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(config.globalDashboardTimeoutMinutes()))
                .maximumSize(config.keyFiguresTimeoutHours())
                .build();

        this.keyFigures = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(config.globalDashboardTimeoutMinutes()))
                .maximumSize(config.keyFiguresTimeoutHours())
                .build();
    }

}
