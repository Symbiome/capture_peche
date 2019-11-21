package fr.inra.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.inra.fishola.database.UserDao;
import fr.inra.fishola.database.UserProfile;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Path("/api/v1/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource {

    @Inject
    protected UserDao userDao;

    protected Algorithm getJwtSecretAlgorithm() {
        // TODO AThimel 21/11/2019 Real key
        return Algorithm.HMAC512("v3ry 53cr37 k3y");
    }

    @PUT
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(RegisterBean bean) {

        if (bean == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (StringUtils.isEmpty(bean.firstName) || StringUtils.isEmpty(bean.lastName) || StringUtils.isEmpty(bean.email) || StringUtils.isEmpty(bean.password)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        String passwordHashed = userDao.hashPassword(bean.password);

        Algorithm algorithmHS = getJwtSecretAlgorithm();

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        Date expiresAt = calendar.getTime();
        String token = JWT.create()
                .withIssuer("fishola-backend")
                .withSubject(bean.email)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("firstName", bean.firstName)
                .withClaim("lastName", bean.lastName)
                .withClaim("passwordHashed", passwordHashed)
                .sign(algorithmHS);

        String apiBaseUrl = "http://0.0.0.0:8080";
        String verifyUrl = String.format("%s/api/v1/security/verify?t=%s", apiBaseUrl, token);

        System.out.println("Verify URL is: " + verifyUrl);

        return Response.ok(verifyUrl, MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/verify")
    public Response verifyAfterRegistration(@QueryParam("t") String token) {

        Algorithm algorithmHS = getJwtSecretAlgorithm();
        DecodedJWT verify = JWT.require(algorithmHS)
                .withIssuer("fishola-backend")
                .build()
                .verify(token);

        String email = verify.getSubject();

        userDao.create(
                verify.getClaim("firstName").asString(),
                verify.getClaim("lastName").asString(),
                email,
                verify.getClaim("passwordHashed").asString()
        );

        return Response.ok().build();
    }

    @GET
    @Path("/login")
    public Response login(@QueryParam("email") String email,
                          @QueryParam("password") String password) {

        boolean authenticate = userDao.authenticate(email, password);
        if (authenticate) {
            Algorithm algorithmHS = getJwtSecretAlgorithm();

//        iss issuer : qui a émis le token
//        sub subject : identifiant unique métier
//        aud audience : fishola mobile ?
//        exp date d'expritration
//        nbf not before
//        iat issued at
//        jti identifiant unique : uuid

            Date now = new Date();
            String token = JWT.create()
                    .withIssuer("fishola-backend")
                    .withSubject(email)
                    .withIssuedAt(now)
                    .withJWTId(UUID.randomUUID().toString())
                    .sign(algorithmHS);

            NewCookie cookie = new NewCookie("token", token);

            // FIXME AThimel 21/11/2019 Secure + HTTPOnly
            return Response.ok().cookie(cookie).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    @GET
    @Path("/profile")
    public UserProfile getProfile(@CookieParam("token") Cookie cookie) {

        Algorithm algorithmHS = getJwtSecretAlgorithm();

        try {
            if (cookie == null) {
                throw new NotAuthenticatedException();
            }
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .build()
                    .verify(cookie.getValue());
            String email = verify.getSubject();
            UserProfile result = userDao.loadUser(email).orElseThrow(NotAuthenticatedException::new);
            return result;
        } catch (JWTVerificationException ve) {
            ve.printStackTrace();
            throw new NotAuthenticatedException();
        }

    }

}
