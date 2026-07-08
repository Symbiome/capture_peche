package fr.inrae.fishola.rest;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.rest.about.KeyFigures;
import fr.inrae.fishola.rest.dashboard.EvolutionMetricsForWaterEntity;
import fr.inrae.fishola.rest.dashboard.GlobalDashboard;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
public class FisholaCache {

    public final Cache<String, GlobalDashboard> globalDashboard;

    public final Cache<String, EvolutionMetricsForWaterEntity> globalEvolution;

    public final Cache<String, EvolutionMetricsForWaterEntity> personalEvolution;


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
