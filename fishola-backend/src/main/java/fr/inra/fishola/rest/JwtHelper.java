package fr.inra.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.exceptions.NotAuthenticatedException;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@RequestScoped
public class JwtHelper {

    @Inject
    protected FisholaConfiguration config;

    public Algorithm getJwtSecretAlgorithm() {
        String jwtSecret = config.getJwtSecret();
        Algorithm result = Algorithm.HMAC512(jwtSecret);
        return result;
    }

    public UUID tokenToUserID(String token) throws NotAuthenticatedException {

        try {
            if (StringUtils.isEmpty(token)) {
                throw new NotAuthenticatedException("Cookie manquant");
            }
            Algorithm algorithmHS = getJwtSecretAlgorithm();
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .build()
                    .verify(token);
            String subject = verify.getSubject();

            if (StringUtils.isEmpty(subject)) {
                throw new NotAuthenticatedException("Subject manquant");
            }

            UUID result = UUID.fromString(subject);
            return result;
        } catch (JWTVerificationException | IllegalArgumentException eee) {
            throw new NotAuthenticatedException("Token invalide", eee);
        }
    }

    public String createToken(UUID userId) {

//        iss issuer : qui a émis le token
//        sub subject : identifiant unique métier
//        aud audience : fishola mobile ?
//        exp date d'expritration
//        nbf not before
//        iat issued at
//        jti identifiant unique : uuid

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        Date expiresAt = calendar.getTime();

        Algorithm algorithmHS = getJwtSecretAlgorithm();
        String result = JWT.create()
                .withIssuer("fishola-backend")
                .withSubject(userId.toString())
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithmHS);

        return result;
    }

}
