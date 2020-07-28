package fr.inrae.fishola.rest;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import com.auth0.jwt.exceptions.TokenExpiredException;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.exceptions.AccessDeniedException;
import fr.inrae.fishola.exceptions.NotAuthenticatedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.CookieParam;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.Optional;
import java.util.UUID;

import static javax.transaction.Transactional.TxType.REQUIRED;
import static javax.ws.rs.core.Cookie.DEFAULT_VERSION;
import static javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

@Transactional(REQUIRED)
public abstract class AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(AbstractFisholaResource.class);

    public static final String USER_AUTHENTICATION_COOKIE_NAME = "X-Fishola-Token";

    public static final String ADMIN_AUTHENTICATION_COOKIE_NAME = "X-Fishola-Admin-Token";

    @Inject
    protected JwtHelper jwtHelper;

    @Inject
    protected UsersDao usersDao;

    @Inject
    protected FisholaConfiguration config;

    @CookieParam(USER_AUTHENTICATION_COOKIE_NAME)
    protected String userToken;

    @CookieParam(ADMIN_AUTHENTICATION_COOKIE_NAME)
    protected String adminToken;

    protected NewCookie createUserTokenCookie(String token) {
        NewCookie result = newTokenCookie(USER_AUTHENTICATION_COOKIE_NAME, token, DEFAULT_MAX_AGE);
        return result;
    }

    protected NewCookie dropUserTokenCookie() {
        NewCookie result = newTokenCookie(USER_AUTHENTICATION_COOKIE_NAME, "invalid-to-make-sure-logout", DEFAULT_MAX_AGE);
        return result;
    }

    protected NewCookie createAdminTokenCookie(String token) {
        NewCookie result = newTokenCookie(ADMIN_AUTHENTICATION_COOKIE_NAME, token, DEFAULT_MAX_AGE);
        return result;
    }

    protected NewCookie dropAdminTokenCookie() {
        NewCookie result = newTokenCookie(ADMIN_AUTHENTICATION_COOKIE_NAME, "invalid-to-make-sure-logout", DEFAULT_MAX_AGE);
        return result;
    }

    private NewCookie newTokenCookie(String tokenName, String token, int maxAge) {
        // XXX AThimel 15/06/2020 Ça pourrait être problématique pour faire tourner un autre profil que "dev" sur une IP locale
        boolean secure = false;
        NewCookie result = new NewCookie(
                tokenName,
                token,
                "/api",
                null,
                DEFAULT_VERSION,
                null,
                maxAge,
                null,
                secure,
                true);
        return result;
    }

    /**
     * Vérifie le token contenu dans le cookie et tente un renouvelement le cas échéant. Le cookie utilisé est celui
     * injecté dans {@link AbstractFisholaResource#userToken} : X-Fishola-Token
     * @return L'identifiant de l'utilisateur et éventuellement le nouveau token s'il y a eu renouvellement
     */
    protected UserIdAndRenewal getUserIdOrRenew() {
        try {
            UUID userId = jwtHelper.verifyToken(userToken);
            // Now we have to check that the userId is valid
            if (!usersDao.isValidUserId(userId)) {
                throw new NotAuthenticatedException("Utilisateur inconnu");
            }
            UserIdAndRenewal result = UserIdAndRenewal.of(userId);
            return result;
        } catch (TokenExpiredException tee) {
            Optional<UserIdAndRenewal> renewal = tryTokenRenewal(userToken);
            if (renewal.isPresent()) {
                return renewal.get();
            } else {
                throw new NotAuthenticatedException("Token non renouvelable", tee);
            }
        } catch (RuntimeException re) {
            throw new NotAuthenticatedException("Token invalide", re);
        }
    }

    /**
     * Dans le cas d'un token expiré on tente de le renouveller
     * @param token le token expiré
     * @return le résultat de la tentative. Optional.empty() si le renouvellement a échoué.
     */
    protected Optional<UserIdAndRenewal> tryTokenRenewal(String token) {
        try {
            UUID userId = jwtHelper.verifyExpiredToken(token);
            boolean isValid = usersDao.isValidUserId(userId);
            if (isValid) {
                if (log.isInfoEnabled()) {
                    log.info("Renouvellement automatique du token JWT pour l'utilisateur " + userId);
                }
                String newToken = jwtHelper.createUserToken(userId);
                UserIdAndRenewal result = UserIdAndRenewal.of(userId, newToken);
                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        } catch (Exception eee) {
            if (log.isWarnEnabled()) {
                log.warn("Le renouvellement de Token n'est pas possible", eee);
            }
            return Optional.empty();
        }
    }

    protected void checkIsAdmin() throws NotAuthenticatedException, AccessDeniedException {
        if (adminToken == null) {
            throw new NotAuthenticatedException("Il faut d'abord s'authentifier");
        }
        boolean validToken = jwtHelper.isValidToken(adminToken);
        AccessDeniedException.check(validToken, "Accès refusé");
    }

    protected StreamingOutput wrapAsStreamingOutput(byte[] array) {
        return output -> {
            output.write(array);
            output.flush();
        };
    }

    protected Response noContent(UserIdAndRenewal userIdAndRenewal) {
        Response.ResponseBuilder responseBuilder = Response.noContent();
        Response result = buildResponse(responseBuilder, userIdAndRenewal);
        return result;
    }

    protected Response wrapEntity(Object entity, UserIdAndRenewal userIdAndRenewal) {
        Response.ResponseBuilder responseBuilder = Response.ok(entity);
        Response result = buildResponse(responseBuilder, userIdAndRenewal);
        return result;
    }

    protected Response buildResponse(Response.ResponseBuilder responseBuilder, UserIdAndRenewal userIdAndRenewal) {
        userIdAndRenewal.renewalToken()
                .map(this::createUserTokenCookie)
                .ifPresent(responseBuilder::cookie);
        Response result = responseBuilder.build();
        return result;
    }

}
