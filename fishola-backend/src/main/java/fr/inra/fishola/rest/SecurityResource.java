package fr.inra.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.database.UserDao;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;
import org.apache.commons.lang3.StringUtils;
import org.jooq.exception.DataAccessException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource {

    @Inject
    protected UserDao userDao;

    @Inject
    protected FisholaConfiguration config;

    protected Algorithm getJwtSecretAlgorithm() {
        String jwtSecret = config.getJwtSecret();
        Algorithm result = Algorithm.HMAC512(jwtSecret);
        return result;
    }

    @PUT
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegisterBean bean) {

        if (bean == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Map<String, String> validationErrors = new HashMap<>();

        if (StringUtils.isEmpty(bean.firstName)) {
            validationErrors.put("firstName", "Le prénom est obligatoire");
        }

        if (StringUtils.isEmpty(bean.lastName)) {
            validationErrors.put("lastName", "Le nom est obligatoire");
        }

        if (StringUtils.isEmpty(bean.email)) {
            validationErrors.put("email", "L'e-mail est obligatoire");
        } else if (loadUser(bean.email).isPresent()) {
            // On vérifie qu'il n'y a pas déjà un compte avec cet email
            validationErrors.put("email", "E-mail déjà utilisé");
        }

        if (StringUtils.isEmpty(bean.password)) {
            validationErrors.put("password", "Le mot de passe est obligatoire");
        } else if (bean.password.length() < 6) {
            validationErrors.put("password", "Le mot de passe doit comporter au moins 6 caractères");
        }

        if (!validationErrors.isEmpty()) {
            Response response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(validationErrors)
                    .build();
            return response;
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

        String apiBaseUrl = config.getBackendBaseUrl();
        String verifyUrl = String.format("%s/api/v1/security/verify?t=%s", apiBaseUrl, token);

        System.out.println("Verify URL is: " + verifyUrl);

        return Response.ok().build();
    }

    @GET
    @Path("/verify")
    public Response verifyAfterRegistration(@QueryParam("t") String token) {

        try {
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

            // TODO: 22/11/2019 Réponse adaptée
            return Response.ok().build();
        } catch (TokenExpiredException | InvalidClaimException tee) {
            throw new NotAuthenticatedException();
        } catch (DataAccessException dae) {
            // TODO: 22/11/2019 Réponse adaptée
            return Response.ok().build();
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginBean bean) {

        Optional<Boolean> authenticate = userDao.authenticate(bean.email, bean.password);
        if (authenticate.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (authenticate.get()) {
            Algorithm algorithmHS = getJwtSecretAlgorithm();

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

            String token = JWT.create()
                    .withIssuer("fishola-backend")
                    .withSubject(bean.email)
                    .withIssuedAt(now)
                    .withExpiresAt(expiresAt)
                    .withJWTId(UUID.randomUUID().toString())
                    .sign(algorithmHS);

            NewCookie cookie = new NewCookie("token", token);

            // FIXME AThimel 21/11/2019 Secure + HTTPOnly
            Response result = Response.ok().cookie(cookie).build();
            return result;
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    @GET
    @Path("/logout")
    public Response logout(@CookieParam("token") Cookie cookie) {
        NewCookie dropCookie = new NewCookie("token", "");
        Response result = Response.ok().cookie(dropCookie).build();
        return result;
    }

    protected UserProfile toUserProfile(FisholaUser input) {
        ImmutableUserProfile result = ImmutableUserProfile.builder()
                .email(input.getEmail())
                .firstName(input.getFirstName())
                .lastName(Optional.ofNullable(input.getLastName()))
                .birthYear(Optional.ofNullable(input.getBirthYear()))
                .gender(Optional.ofNullable(input.getGender()))
                .build();
        return result;
    }

    protected Optional<UserProfile> loadUser(String email) {
        Optional<FisholaUser> user = userDao.findByEmail(email);
        Optional<UserProfile> result = user.map(this::toUserProfile);
        return result;
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
            UserProfile result = loadUser(email).orElseThrow(NotAuthenticatedException::new);
            return result;
        } catch (JWTVerificationException ve) {
            ve.printStackTrace();
            throw new NotAuthenticatedException();
        }

    }

}
