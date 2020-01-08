package fr.inra.fishola.rest.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Preconditions;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;
import fr.inra.fishola.exceptions.FisholaTechnicalException;
import fr.inra.fishola.exceptions.NotAuthenticatedException;
import fr.inra.fishola.mails.FisholaMail;
import fr.inra.fishola.mails.ImmutableFisholaMail;
import fr.inra.fishola.mails.MailService;
import fr.inra.fishola.rest.AbstractFisholaResource;
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

@Path("/api/v1/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(SecurityResource.class);

    @Inject
    protected FisholaConfiguration config;

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

        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1);
        Date expiresAt = calendar.getTime();

        Algorithm algorithmHS = jwtHelper.getJwtSecretAlgorithm();

        String token = JWT.create()
                .withIssuer("fishola-backend")
                .withSubject("register")
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("email", bean.email)
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
            Algorithm algorithmHS = jwtHelper.getJwtSecretAlgorithm();
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .withSubject("register")
                    .build()
                    .verify(token);

            String email = verify.getSubject();

            if (log.isInfoEnabled()) {
                log.info(String.format("Email verified, create account for %s", email));
            }

            usersDao.create(
                    verify.getClaim("firstName").asString(),
                    verify.getClaim("lastName").asString(),
                    verify.getClaim("email").asString(),
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

            Optional<FisholaUser> byEmail = usersDao.findByEmail(bean.email);
            Preconditions.checkState(byEmail.isPresent());
            UUID userId = byEmail.get().getId();

            String token = jwtHelper.createToken(userId);

            NewCookie loginCookie = createTokenCookie(token);
            Response result = Response.ok().cookie(loginCookie).build();
            return result;
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    @GET
    @Path("/logout")
    public Response logout(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        // Pour le logout on va générer un cookie qui va écraser/effacer le cookie normal
        NewCookie logoutCookie = dropTokenCookie();
        Response result = Response.ok().cookie(logoutCookie).build();
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
    public UserProfile getProfile(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        UUID userId = getUserId(cookie);
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});
        UserProfile result = toUserProfile(user);
        return result;
    }

}
