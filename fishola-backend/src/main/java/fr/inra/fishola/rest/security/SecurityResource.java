package fr.inra.fishola.rest.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.exceptions.FisholaTechnicalException;
import fr.inra.fishola.database.UsersDao;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;
import fr.inra.fishola.mails.FisholaMail;
import fr.inra.fishola.mails.ImmutableFisholaMail;
import fr.inra.fishola.mails.MailService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.exception.DataAccessException;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
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

import static javax.ws.rs.core.Cookie.DEFAULT_VERSION;
import static javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

@Path("/api/v1/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource {

    private static final Log log = LogFactory.getLog(SecurityResource.class);

    @Inject
    protected UsersDao usersDao;

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected AuthenticationService authenticationService;

    @Inject
    protected MailService mailService;

    @PUT
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(RegisterBean bean, @Context HttpServletRequest request) {

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
        } else if (!isEmailInValidFormat(bean.email)) {
            // On vérifie qu'il n'y a pas déjà un compte avec cet email
            validationErrors.put("email", "Le format n'est pas correct");
        } else if (usersDao.findByEmail(bean.email).isPresent()) {
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

        String passwordHashed = usersDao.hashPassword(bean.password);

        Algorithm algorithmHS = authenticationService.getJwtSecretAlgorithm();

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

        String apiBaseUrl = config.getApiUrl("/api/v1/security/verify", request);
        String verifyUrl = String.format("%s?t=%s", apiBaseUrl, token);

        ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                "emails/email-validation.html",
                "verifyLink", verifyUrl,
                "firstName", bean.firstName);
        FisholaMail mail = builder
                .addTos(bean.email)
                .subject("Fishola - Validation de votre e-mail")
                .build();
        // FIXME AThimel 20/12/2019 L'envoi de mail doit se faire en asynchrone ou bien il faut gérer les erreurs
        mailService.sendMail(mail);

        // FIXME AThimel 20/12/2019 Pour les besoins de la démo, on active d'office les comptes
        try {
            verifyAfterRegistration(token);
        } catch (Exception eee) {
            log.error("Unable to verify token", eee);
        }

        return Response.ok().build();
    }

    protected boolean isEmailInValidFormat(String email) {
        try {
            InternetAddress internetAddress = new InternetAddress(email);
            internetAddress.validate();
            return true;
        } catch (AddressException ex) {
            if (log.isInfoEnabled()) {
                log.info(String.format("'%s' does not seem to be a valid email address", email));
            }
            return false;
        }
    }

    @GET
    @Path("/verify")
    public Response verifyAfterRegistration(@QueryParam("t") String token) {

        try {
            Algorithm algorithmHS = authenticationService.getJwtSecretAlgorithm();
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .build()
                    .verify(token);

            String email = verify.getSubject();

            if (log.isInfoEnabled()) {
                log.info(String.format("Email verified, create account for %s", email));
            }

            usersDao.create(
                    verify.getClaim("firstName").asString(),
                    verify.getClaim("lastName").asString(),
                    email,
                    verify.getClaim("passwordHashed").asString()
            );

            // TODO: 22/11/2019 Réponse adaptée
            return Response.ok().build();
        } catch (TokenExpiredException | InvalidClaimException tee) {
            throw new FisholaTechnicalException("Unable to register", tee);
        } catch (DataAccessException dae) {
            // TODO: 22/11/2019 Réponse adaptée
            return Response.ok().build();
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(LoginBean bean) {

        Optional<Boolean> authenticate = usersDao.authenticate(bean.email, bean.password);
        if (authenticate.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (authenticate.get()) {

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

            Algorithm algorithmHS = authenticationService.getJwtSecretAlgorithm();
            String token = JWT.create()
                    .withIssuer("fishola-backend")
                    .withSubject(bean.email)
                    .withIssuedAt(now)
                    .withExpiresAt(expiresAt)
                    .withJWTId(UUID.randomUUID().toString())
                    .sign(algorithmHS);

            NewCookie cookie = new NewCookie(
                    AuthenticationService.AUTHENTICATION_COOKIE_NAME,
                    token,
                    "/api",
                    null,
                    DEFAULT_VERSION,
                    null,
                    DEFAULT_MAX_AGE,
                    null,
                    false,
                    false);

            // FIXME AThimel 21/11/2019 Secure + HTTPOnly
            Response result = Response.ok().cookie(cookie).build();
            return result;
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    @GET
    @Path("/logout")
    public Response logout(@CookieParam(AuthenticationService.AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        NewCookie dropCookie = new NewCookie(AuthenticationService.AUTHENTICATION_COOKIE_NAME, "");
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

    @GET
    @Path("/profile")
    public UserProfile getProfile(@CookieParam(AuthenticationService.AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        FisholaUser user = authenticationService.getUser(cookie);
        UserProfile result = toUserProfile(user);
        return result;
    }

}
