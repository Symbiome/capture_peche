package fr.inrae.fishola.rest.audit;

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

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une méthode de ressource JAX-RS comme devant être journalisée dans
 * {@code audit_log} (CdC 3.1.5.1 / RGPD note Q8, cf. analyse #11 §4). Le
 * {@link AuditFilter} associé (name binding) écrit une ligne quand la réponse
 * est un succès (2xx). Les lectures simples ne sont PAS annotées ; seuls les
 * mutations et les exports/générations de fiches le sont.
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Audited {

    /** Nom de l'action journalisée, ex. {@code "admin.update"}, {@code "trip.export"}. */
    String value();

    /** Type d'entité visée, ex. {@code "fishola_admin"}. Vide si non pertinent. */
    String entityType() default "";

    /**
     * Nom du path param portant l'UUID de l'entité visée (ex. {@code "adminId"}),
     * recopié dans {@code audit_log.entity_id}. Vide si aucun.
     */
    String entityIdParam() default "";
}
