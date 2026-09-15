package fr.inrae.fishola.gamification.rules;

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

import com.fasterxml.jackson.databind.JsonNode;
import fr.inrae.fishola.gamification.BadgeRuleStrategy;
import fr.inrae.fishola.gamification.GamificationDao;
import fr.inrae.fishola.gamification.UnlockCandidate;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * « Nouveau record personnel » (#146) : {@code rule_params.scope} distingue les 3 badges
 * de cette famille -- {@code species_size}/{@code species_weight} (record par espèce,
 * toutes espèces confondues pour ce badge unique) et {@code session_catch_count} (record
 * du nombre de captures en une session). Contrairement aux autres stratégies, celle-ci est
 * réévaluée même déjà débloquée : le contexte est mis à jour à chaque nouveau record battu
 * (cf. {@link GamificationEngine#evaluateForUser}).
 */
@Singleton
public class MetricRecordStrategy implements BadgeRuleStrategy {

    @Inject
    GamificationDao dao;

    @Override
    public String ruleType() {
        return "metric_record";
    }

    @Override
    public Optional<UnlockCandidate> evaluate(GamificationDao.BadgeRow badge, UUID userId) {
        String scope = RuleParams.stringParam(badge.ruleParams(), "scope", "species_size");
        return switch (scope) {
            case "species_weight" -> evaluatePerSpecies(badge, userId, dao.maxWeightPerSpecies(userId), "weightG");
            case "session_catch_count" -> evaluateSessionCatchCount(badge, userId);
            default -> evaluatePerSpecies(badge, userId, dao.maxSizePerSpecies(userId), "sizeCm");
        };
    }

    /** Cherche, parmi toutes les espèces, celle dont le record vient d'être amélioré. */
    private Optional<UnlockCandidate> evaluatePerSpecies(GamificationDao.BadgeRow badge, UUID userId,
                                                          Map<String, Integer> currentMaxPerSpecies, String valueKey) {
        JsonNode previousContext = dao.findUnlock(badge.id(), userId, (short) 0)
                .map(GamificationDao.UnlockRow::context).orElse(null);
        Map<String, Integer> previousPerSpecies = readPerSpeciesMap(previousContext);

        String improvedSpecies = null;
        int improvedValue = 0;
        for (Map.Entry<String, Integer> entry : currentMaxPerSpecies.entrySet()) {
            Integer previous = previousPerSpecies.get(entry.getKey());
            if (previous == null || entry.getValue() > previous) {
                if (improvedSpecies == null || entry.getValue() > improvedValue) {
                    improvedSpecies = entry.getKey();
                    improvedValue = entry.getValue();
                }
            }
        }
        if (improvedSpecies == null) {
            return Optional.empty();
        }
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("perSpeciesMax", currentMaxPerSpecies);
        newContext.put("species", improvedSpecies);
        newContext.put(valueKey, improvedValue);
        return Optional.of(UnlockCandidate.recordUpdate(newContext));
    }

    private Optional<UnlockCandidate> evaluateSessionCatchCount(GamificationDao.BadgeRow badge, UUID userId) {
        Optional<Integer> currentMax = dao.maxCatchCountInSingleTrip(userId);
        if (currentMax.isEmpty()) {
            return Optional.empty();
        }
        JsonNode previousContext = dao.findUnlock(badge.id(), userId, (short) 0)
                .map(GamificationDao.UnlockRow::context).orElse(null);
        int previousValue = RuleParams.intParam(previousContext, "catchCount", -1);
        if (currentMax.get() <= previousValue) {
            return Optional.empty();
        }
        return Optional.of(UnlockCandidate.recordUpdate(Map.of("catchCount", currentMax.get())));
    }

    private Map<String, Integer> readPerSpeciesMap(JsonNode context) {
        Map<String, Integer> result = new HashMap<>();
        if (context == null || !context.has("perSpeciesMax")) {
            return result;
        }
        context.get("perSpeciesMax").fields()
                .forEachRemaining(e -> result.put(e.getKey(), e.getValue().asInt()));
        return result;
    }
}
