package fr.inra.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.database.UserDao;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import java.util.Optional;
import java.util.UUID;

@RequestScoped
public class AuthenticationService {

    public static final String AUTHENTICATION_COOKIE_NAME = "token";

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected UserDao userDao;

    public Algorithm getJwtSecretAlgorithm() {
        String jwtSecret = config.getJwtSecret();
        Algorithm result = Algorithm.HMAC512(jwtSecret);
        return result;
    }

    public FisholaUser getUser(Cookie cookie) throws NotAuthenticatedException {

        try {
            if (cookie == null) {
                throw new NotAuthenticatedException();
            }
            Algorithm algorithmHS = getJwtSecretAlgorithm();
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .build()
                    .verify(cookie.getValue());
            String email = verify.getSubject();

            Optional<FisholaUser> user = userDao.findByEmail(email);
            FisholaUser result = user.orElseThrow(NotAuthenticatedException::new);
            return result;
        } catch (JWTVerificationException ve) {
            ve.printStackTrace();
            throw new NotAuthenticatedException();
        }
    }

    public UUID getUserId(Cookie cookie) throws NotAuthenticatedException {
        FisholaUser user = getUser(cookie);
        UUID result = user.getId();
        return result;
    }

}
