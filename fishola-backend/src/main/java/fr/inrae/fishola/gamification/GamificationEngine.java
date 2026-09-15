package fr.inrae.fishola.gamification;

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

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Socle de gamification (#7, construit à l'occasion de #146) : réévalue, pour un pêcheur
 * donné, l'ensemble des badges actifs et persiste les nouveaux déblocages. Appelé de
 * façon synchrone (pas de bus d'événements dans ce codebase, cf. recherche #146) à la fin
 * des points d'écriture qui créent/modifient les captures d'un pêcheur (uniquement
 * {@code TripResource} : les sorties saisies par un opérateur ont {@code owner_id IS
 * NULL} et ne déclenchent donc jamais de badge -- ce n'est pas un oubli, cf. commentaire
 * sur {@link #evaluateForUser}).
 */
@Singleton
public class GamificationEngine {

    @Inject
    GamificationDao dao;

    @Inject
    Instance<BadgeRuleStrategy> strategyInstances;

    private Map<String, BadgeRuleStrategy> strategiesByRuleType;

    @PostConstruct
    void init() {
        strategiesByRuleType = new HashMap<>();
        for (BadgeRuleStrategy strategy : strategyInstances) {
            strategiesByRuleType.put(strategy.ruleType(), strategy);
        }
    }

    /**
     * Réévalue tous les badges actifs pour ce pêcheur. No-op si {@code fishowaUserId} est
     * nul : c'est le cas pour toutes les sorties saisies par un opérateur (import/saisie
     * manuelle), qui n'ont pas de {@code trip.owner_id} -- seul le chemin pêcheur mobile
     * ({@code TripResource}) fournit un identifiant.
     */
    public void evaluateForUser(UUID fishowaUserId) {
        if (fishowaUserId == null) {
            return;
        }
        short currentYear = (short) Year.now().getValue();
        Set<UUID> unlockedLifetime = dao.unlockedBadgeIds(fishowaUserId, (short) 0);
        Set<UUID> unlockedThisYear = dao.unlockedBadgeIds(fishowaUserId, currentYear);

        for (GamificationDao.BadgeRow badge : dao.listActiveBadges()) {
            if ("manual".equals(badge.ruleType())) {
                continue;
            }
            BadgeRuleStrategy strategy = strategiesByRuleType.get(badge.ruleType());
            if (strategy == null) {
                continue;
            }
            short periodYear = badge.annualReset() ? currentYear : 0;
            Set<UUID> alreadyUnlocked = badge.annualReset() ? unlockedThisYear : unlockedLifetime;
            // Les badges "record" sont réévalués même débloqués : ils peuvent être mis à
            // jour avec une nouvelle valeur (nouvelle taille/poids/nombre battant l'ancien).
            boolean mayNeedUpdate = "metric_record".equals(badge.ruleType());
            if (alreadyUnlocked.contains(badge.id()) && !mayNeedUpdate) {
                continue;
            }

            Optional<UnlockCandidate> candidate = strategy.evaluate(badge, fishowaUserId);
            candidate.ifPresent(c -> dao.upsertUnlock(
                    badge.id(), fishowaUserId, periodYear, c.context(), c.updateIfExists(), null));
        }
    }
}
