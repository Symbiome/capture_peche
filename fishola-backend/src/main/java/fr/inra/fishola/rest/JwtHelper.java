package fr.inra.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.database.UsersDao;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;
import fr.inra.fishola.exceptions.NotAuthenticatedException;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

@RequestScoped
public class JwtHelper {

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected UsersDao usersDao;

    public Algorithm getJwtSecretAlgorithm() {
        String jwtSecret = config.getJwtSecret();
        Algorithm result = Algorithm.HMAC512(jwtSecret);
        return result;
    }

    private FisholaUser getUser(String token) throws NotAuthenticatedException {

        try {
            if (StringUtils.isEmpty(token)) {
                throw new NotAuthenticatedException("Cookie manquant");
            }
            Algorithm algorithmHS = getJwtSecretAlgorithm();
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .build()
                    .verify(token);
            String email = verify.getSubject();

            Optional<FisholaUser> user = usersDao.findByEmail(email);
            FisholaUser result = user.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});
            return result;
        } catch (JWTVerificationException ve) {
            ve.printStackTrace();
            throw new NotAuthenticatedException("Token invalide");
        }
    }

    public UUID getUserId(String token) throws NotAuthenticatedException {
        FisholaUser user = getUser(token);
        UUID result = user.getId();
        return result;
    }

}
