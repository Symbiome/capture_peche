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

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.inrae.fishola.database.AuditLogDao;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.JwtHelper;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Écrit une ligne d'{@code audit_log} pour chaque méthode annotée {@code @Audited}
 * dont la réponse est un succès (2xx). Name binding : ne s'exécute que sur les
 * endpoints annotés (pas de surcoût global). L'acteur est résolu depuis les
 * cookies JWT déjà utilisés par l'application. Toute erreur d'audit est avalée :
 * le journal ne doit jamais casser la réponse métier (#11 §4).
 */
@Provider
@Audited("")
public class AuditFilter implements ContainerResponseFilter {

    @Inject
    JwtHelper jwtHelper;

    @Inject
    AuditLogDao auditLogDao;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Logger log;

    @Context
    ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        try {
            int status = responseContext.getStatus();
            if (status < 200 || status >= 300) {
                // On n'audite que les succès (cf. décision §7.4 — les refus 403
                // ne sont pas journalisés par défaut).
                return;
            }
            Method method = resourceInfo.getResourceMethod();
            Audited audited = (method == null) ? null : method.getAnnotation(Audited.class);
            if (audited == null || audited.value().isBlank()) {
                return;
            }

            // Acteur : admin prioritaire, sinon utilisateur, sinon système.
            String actorType = "system";
            UUID actorId = null;
            String adminToken = cookieValue(requestContext, AbstractFisholaResource.ADMIN_AUTHENTICATION_COOKIE_NAME);
            String userToken = cookieValue(requestContext, AbstractFisholaResource.USER_AUTHENTICATION_COOKIE_NAME);
            if (adminToken != null && jwtHelper.isValidToken(adminToken)) {
                actorType = "admin";
                actorId = jwtHelper.verifyToken(adminToken);
            } else if (userToken != null && jwtHelper.isValidToken(userToken)) {
                actorType = "user";
                actorId = jwtHelper.verifyToken(userToken);
            }

            // Entité visée (facultative) depuis un path param.
            String entityType = audited.entityType().isBlank() ? null : audited.entityType();
            UUID entityId = null;
            if (!audited.entityIdParam().isBlank()) {
                String raw = requestContext.getUriInfo().getPathParameters().getFirst(audited.entityIdParam());
                entityId = tryParseUuid(raw);
            }

            // Contexte minimal (minimisation RGPD : pas de donnée personnelle superflue).
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("method", requestContext.getMethod());
            details.put("path", requestContext.getUriInfo().getPath());
            details.put("httpStatus", status);

            auditLogDao.insert(actorType, actorId, audited.value(), entityType, entityId,
                    objectMapper.writeValueAsString(details));
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Échec d'écriture du journal d'audit (réponse métier non impactée)", e);
        }
    }

    private static String cookieValue(ContainerRequestContext ctx, String name) {
        Cookie cookie = ctx.getCookies().get(name);
        return cookie == null ? null : cookie.getValue();
    }

    private static UUID tryParseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
