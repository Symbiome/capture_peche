package fr.inrae.fishola.rest;

import com.auth0.jwt.exceptions.TokenExpiredException;
import fr.inrae.fishola.database.UsersDao;
import fr.inrae.fishola.exceptions.NotAuthenticatedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Cookie;
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

    public static final String AUTHENTICATION_COOKIE_NAME = "token";

    @Inject
    protected JwtHelper jwtHelper;

    @Inject
    protected UsersDao usersDao;

    protected NewCookie createTokenCookie(String token) {
        NewCookie result = newTokenCookie(token, DEFAULT_MAX_AGE);
        return result;
    }

    protected NewCookie dropTokenCookie() {
        NewCookie result = newTokenCookie("", 0);
        return result;
    }

    private NewCookie newTokenCookie(String token, int maxAge) {
        // FIXME AThimel 21/11/2019 Secure + HTTPOnly
        NewCookie result = new NewCookie(
                AUTHENTICATION_COOKIE_NAME,
                token,
                "/api",
                null,
                DEFAULT_VERSION,
                null,
                maxAge,
                null,
                false,
                true);
        return result;
    }

    @Deprecated
    protected UUID getUserId(Cookie cookie) {
        String token = Optional.ofNullable(cookie)
                .map(Cookie::getValue)
                .orElse(null);
        UUID userId = jwtHelper.tokenToUserID(token);
        // Now we have to check that the userId is valid
        if (!usersDao.isValidUserId(userId)) {
            throw new NotAuthenticatedException("Utilisateur inconnu");
        }
        return userId;
    }

    /**
     * Vérifie le token contenu dans le cookie et tente un renouvelement le cas échéant.
     * @param cookie le cookie envoyé par le navigateur
     * @return L'identifiant de l'utilisateur et éventuellement le nouveau token s'il y a eu renouvellement
     */
    protected UserIdAndRenewal getUserIdOrRenew(Cookie cookie) {
        String token = Optional.ofNullable(cookie)
                .map(Cookie::getValue)
                .orElse(null);
        try {
            UUID userId = jwtHelper.verifyToken(token);
            // Now we have to check that the userId is valid
            if (!usersDao.isValidUserId(userId)) {
                throw new NotAuthenticatedException("Utilisateur inconnu");
            }
            UserIdAndRenewal result = UserIdAndRenewal.of(userId);
            return result;
        } catch (TokenExpiredException tee) {
            Optional<UserIdAndRenewal> renewal = tryTokenRenewal(token);
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
                String newToken = jwtHelper.createToken(userId);
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
                .map(this::createTokenCookie)
                .ifPresent(responseBuilder::cookie);
        Response result = responseBuilder.build();
        return result;
    }

}
