package fr.inrae.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.Maps;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import fr.inrae.fishola.exceptions.NotAuthenticatedException;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RequestScoped
public class JwtHelper {

    @Inject
    protected FisholaConfiguration config;

    private Algorithm getJwtSecretAlgorithm() {
        String jwtSecret = config.getJwtSecret();
        Algorithm result = Algorithm.HMAC512(jwtSecret);
        return result;
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

    public String createCustomToken(String subject, int expiresInHours, Map<String, String> claims) {

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expiresInHours);
        Date expiresAt = calendar.getTime();

        Algorithm algorithmHS = getJwtSecretAlgorithm();

        JWTCreator.Builder tokenBuilder = JWT.create()
                .withIssuer("fishola-backend")
                .withSubject(subject)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withJWTId(UUID.randomUUID().toString());
        for (Map.Entry<String, String> entry : claims.entrySet()) {
            tokenBuilder.withClaim(entry.getKey(), entry.getValue());
        }
        return tokenBuilder.sign(algorithmHS);
    }

    public Map<String, String> verifyCustomToken(String subject, String token) {
        try {
            Algorithm algorithmHS = getJwtSecretAlgorithm();
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .withSubject(subject)
                    .build()
                    .verify(token);
            Map<String, Claim> claims = verify.getClaims();
            Map<String, String> result = Maps.transformValues(claims, Claim::asString);
            return result;
        } catch (TokenExpiredException | InvalidClaimException tee) {
            throw new FisholaTechnicalException("Unable to register", tee);
        }
    }

}
