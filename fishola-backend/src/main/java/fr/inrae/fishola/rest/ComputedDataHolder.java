package fr.inrae.fishola.rest;

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
