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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

public class ComputedDataHolder<T> {

    /**
     * Permet de ne pas faire plusieurs fois le calcul en même temps.
     */
    protected AtomicBoolean runningRefresh = new AtomicBoolean(false);

    /**
     * Dernière instance calculée. Celle-ci va rester en mémoire jusqu'à ce qu'elle expire.
     */
    protected T latest;

    public T get(Supplier<T> newInstanceSupplier,
                 Function<T, LocalDateTime> getComputeDate,
                 Duration thresholdDuration,
                 boolean serveCold) {

        Supplier<T> computeAndSave = () -> {
            T newInstance = newInstanceSupplier.get();
            this.latest = newInstance;
            return newInstance;
        };

        T result = Optional.ofNullable(this.latest)
                .orElseGet(computeAndSave);

        // On calcule l'age de l'instance actuelle
        Duration age = Duration.between(getComputeDate.apply(result), LocalDateTime.now());
        if (age.compareTo(thresholdDuration) > 0) {
            if (serveCold) {
                // Si elle a expiré on demandé son calcul en arrière-plan mais on renvoie quand même la valeur expirée
                asyncRefresh(computeAndSave);
            } else {
                // On demande tout de suite le résultat
                result = computeAndSave.get();
            }
        }

        return result;
    }

    /**
     * Méthode non bloquante qui déclenche la mise à jour de l'instance mais qui n'attend pas que son calcul soit
     * terminé avant de rendre la main
     */
    protected void asyncRefresh(Supplier<T> computeAndSave) {
        boolean isCurrentlyRunning = runningRefresh.compareAndExchange(false, true);
        if (!isCurrentlyRunning) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                computeAndSave.get();
                runningRefresh.set(false);
            });
        }
    }

    public void unset() {
        this.latest = null;
    }

}
