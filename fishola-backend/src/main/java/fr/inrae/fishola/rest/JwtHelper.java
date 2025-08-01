package fr.inrae.fishola.rest;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import fr.inrae.fishola.FisholaConfiguration;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RequestScoped
public class JwtHelper {

    @Inject
    protected Logger log;

    @Inject
    protected FisholaConfiguration config;

    private static final String FISHOLA_BACKEND_ISSUER = "fishola-backend";
    private static final String MISSING_TOKEN_ERR_MESSAGE = "Token manquant";
    private static final String MISSING_SUBJECT_ERR_MESSAGE = "Subject manquant";

    private Algorithm getJwtSecretAlgorithm() {
        String jwtSecret = config.jwtSecret();
        Algorithm result = Algorithm.HMAC512(jwtSecret);
        return result;
    }

    public String createUserToken(UUID userId) {

        if (log.isInfoEnabled()) {
            log.infof("Création d'un token JWT pour l'utilisateur %s", userId);
        }

        String result = createToken0(userId);
        return result;
    }

    private String createToken0(UUID userId) {

//        iss issuer : qui a émis le token
//        sub subject : identifiant unique métier
//        aud audience : fishola mobile ?
//        exp date d'expritration
//        nbf not before
//        iat issued at
//        jti identifiant unique : uuid

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, config.jwtLifetimeHours());
        Date expiresAt = calendar.getTime();

        Algorithm algorithmHS = getJwtSecretAlgorithm();
        return JWT.create()
                .withIssuer(FISHOLA_BACKEND_ISSUER)
                .withSubject(userId.toString())
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithmHS);
    }

    public UUID verifyToken(String token) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(token), MISSING_TOKEN_ERR_MESSAGE);

        Algorithm algorithmHS = getJwtSecretAlgorithm();
        DecodedJWT verify = JWT.require(algorithmHS)
                .withIssuer(FISHOLA_BACKEND_ISSUER)
                .build()
                .verify(token);
        String subject = verify.getSubject();

        Preconditions.checkState(StringUtils.isNotEmpty(subject), MISSING_SUBJECT_ERR_MESSAGE);

        UUID result = UUID.fromString(subject);
        return result;
    }

    public UUID verifyExpiredToken(String token) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(token), MISSING_TOKEN_ERR_MESSAGE);

        // On convertit en secondes
        long seconds = (long) config.jwtRenewalHours() * 60 * 60;

        Algorithm algorithmHS = getJwtSecretAlgorithm();
        DecodedJWT verify = JWT.require(algorithmHS)
                .withIssuer(FISHOLA_BACKEND_ISSUER)
                .acceptExpiresAt(seconds)
                .build()
                .verify(token);
        String subject = verify.getSubject();

        Preconditions.checkState(StringUtils.isNotEmpty(subject), MISSING_SUBJECT_ERR_MESSAGE);

        UUID result = UUID.fromString(subject);
        return result;
    }

    public String createCustomToken(String subject, int expiresInHours, Map<String, String> claims) {

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expiresInHours);
        Date expiresAt = calendar.getTime();

        Algorithm algorithmHS = getJwtSecretAlgorithm();

        JWTCreator.Builder tokenBuilder = JWT.create()
                .withIssuer(FISHOLA_BACKEND_ISSUER)
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
                    .withIssuer(FISHOLA_BACKEND_ISSUER)
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

    public boolean isValidToken(String token) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(token), MISSING_TOKEN_ERR_MESSAGE);

        try {
            Algorithm algorithmHS = getJwtSecretAlgorithm();
            JWT.require(algorithmHS)
                    .withIssuer(FISHOLA_BACKEND_ISSUER)
                    .build()
                    .verify(token);
            return true;
        } catch (Exception eee) {
            log.warnf("Token invalide: %s", token, eee);
            return false;
        }
    }

    public String createAdminToken(UUID adminID) {

        if (log.isInfoEnabled()) {
            log.info("Création d'un token admin (pour l'admin " + adminID + ")");
        }
        String result = createToken0(adminID);
        return result;

    }

}
