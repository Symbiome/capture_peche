package fr.inrae.fishola.database;

/*-
 * #%L
 * Carnet de pêche :: Backend
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

import fr.inrae.fishola.entities.enums.HideableReferential;
import fr.inrae.fishola.entities.tables.pojos.Species;
import fr.inrae.fishola.entities.tables.pojos.Technique;
import jakarta.inject.Singleton;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static fr.inrae.fishola.entities.Tables.FISHOLA_ADMIN_HIDDEN_REFERENTIAL;
import static fr.inrae.fishola.entities.Tables.SPECIES;
import static fr.inrae.fishola.entities.Tables.TECHNIQUE;

/**
 * Éléments du référentiel national masqués dans les listes de saisie d'un compte staff
 * (#239). Seuls les masqués sont stockés : un nouvel élément national est affiché par
 * défaut. Le masquage n'est qu'une aide à la saisie, jamais une règle de validation.
 */
@Singleton
public class AdminHiddenReferentialsDao extends AbstractCarnetDao {

    /**
     * Éléments masqués par un compte pour un référentiel.
     *
     * @param adminId compte staff
     * @param type référentiel concerné
     * @return identifiants des éléments masqués
     */
    public Set<UUID> listHidden(UUID adminId, HideableReferential type) {
        return withContext(context -> new HashSet<>(context
                .select(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.ITEM_ID)
                .from(FISHOLA_ADMIN_HIDDEN_REFERENTIAL)
                .where(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.FISHOLA_ADMIN_ID.eq(adminId))
                .and(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.REFERENTIAL_TYPE.eq(type))
                .fetch(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.ITEM_ID)));
    }

    /**
     * Remplace l'ensemble des éléments masqués d'un compte pour un référentiel.
     *
     * @param adminId compte staff
     * @param type référentiel concerné
     * @param itemIds nouveaux éléments masqués, vide pour tout afficher
     */
    public void replaceHidden(UUID adminId, HideableReferential type, Collection<UUID> itemIds) {
        withContextNoResult(context -> {
            context.deleteFrom(FISHOLA_ADMIN_HIDDEN_REFERENTIAL)
                    .where(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.FISHOLA_ADMIN_ID.eq(adminId))
                    .and(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.REFERENTIAL_TYPE.eq(type))
                    .execute();
            var insert = context.insertInto(FISHOLA_ADMIN_HIDDEN_REFERENTIAL,
                    FISHOLA_ADMIN_HIDDEN_REFERENTIAL.FISHOLA_ADMIN_ID,
                    FISHOLA_ADMIN_HIDDEN_REFERENTIAL.REFERENTIAL_TYPE,
                    FISHOLA_ADMIN_HIDDEN_REFERENTIAL.ITEM_ID);
            itemIds.forEach(itemId -> insert.values(adminId, type, itemId));
            if (!itemIds.isEmpty()) {
                insert.onConflictDoNothing().execute();
            }
        });
    }

    /**
     * Identifiants absents du référentiel.
     *
     * @param type référentiel concerné
     * @param itemIds identifiants à contrôler
     * @return identifiants qui ne correspondent à aucun élément du référentiel
     */
    public Set<UUID> unknownItemIds(HideableReferential type, Collection<UUID> itemIds) {
        Set<UUID> unknown = new HashSet<>(itemIds);
        if (!unknown.isEmpty()) {
            Field<UUID> idField = idFieldOf(type);
            unknown.removeAll(withContext(context -> context
                    .select(idField)
                    .from(tableOf(type))
                    .where(idField.in(itemIds))
                    .fetch(idField)));
        }
        return unknown;
    }

    /**
     * Espèces nationales proposées à la saisie d'un compte : ni archivées, ni masquées.
     *
     * @param adminId compte staff
     * @return espèces affichées, triées par nom
     */
    public List<Species> listVisibleSpecies(UUID adminId) {
        return withContext(context -> context
                .selectFrom(SPECIES)
                .where(SPECIES.BUILT_IN.isTrue())
                .and(SPECIES.ARCHIVED.isFalse())
                .and(SPECIES.ID.notIn(hiddenIds(adminId, HideableReferential.species)))
                .orderBy(SPECIES.NAME)
                .fetchInto(Species.class));
    }

    /**
     * Techniques nationales proposées à la saisie d'un compte : ni archivées, ni masquées.
     *
     * @param adminId compte staff
     * @return techniques affichées, triées par nom
     */
    public List<Technique> listVisibleTechniques(UUID adminId) {
        return withContext(context -> context
                .selectFrom(TECHNIQUE)
                .where(TECHNIQUE.BUILT_IN.isTrue())
                .and(TECHNIQUE.ARCHIVED.isFalse())
                .and(TECHNIQUE.ID.notIn(hiddenIds(adminId, HideableReferential.technique)))
                .orderBy(TECHNIQUE.NAME)
                .fetchInto(Technique.class));
    }

    private static Select<Record1<UUID>> hiddenIds(UUID adminId, HideableReferential type) {
        return DSL
                .select(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.ITEM_ID)
                .from(FISHOLA_ADMIN_HIDDEN_REFERENTIAL)
                .where(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.FISHOLA_ADMIN_ID.eq(adminId))
                .and(FISHOLA_ADMIN_HIDDEN_REFERENTIAL.REFERENTIAL_TYPE.eq(type));
    }

    private static Table<?> tableOf(HideableReferential type) {
        return type == HideableReferential.species ? SPECIES : TECHNIQUE;
    }

    private static Field<UUID> idFieldOf(HideableReferential type) {
        return type == HideableReferential.species ? SPECIES.ID : TECHNIQUE.ID;
    }
}
