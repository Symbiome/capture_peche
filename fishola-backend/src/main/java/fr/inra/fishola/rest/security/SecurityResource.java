package fr.inra.fishola.rest.security;

import com.google.common.base.Preconditions;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Path("/api/v1/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(SecurityResource.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FIRST_NAME = "firstName";
    private static final String CLAIM_LAST_NAME = "lastName";
    private static final String CLAIM_PASSWORD_HASHED = "passwordHashed";

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

        String email = StringUtils.trimToEmpty(bean.email).toLowerCase();
        if (StringUtils.isEmpty(email)) {
            validationErrors.put("email", "L'e-mail est obligatoire");
        } else if (!isEmailInValidFormat(email)) {
            // On vérifie qu'il n'y a pas déjà un compte avec cet email
            validationErrors.put("email", "Le format n'est pas correct");
        } else if (usersDao.findByEmail(email).isPresent()) {
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

        Map<String, String> claims = new HashMap<>();
        claims.put(CLAIM_EMAIL, email);
        claims.put(CLAIM_FIRST_NAME, bean.firstName);
        claims.put(CLAIM_LAST_NAME, bean.lastName);
        claims.put(CLAIM_PASSWORD_HASHED, passwordHashed);

        String token = jwtHelper.createCustomToken("register", 1, claims);

        String apiBaseUrl = config.getApiUrl("/api/v1/security/verify", request);
        String verifyUrl = String.format("%s?t=%s", apiBaseUrl, token);

        ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                "emails/email-validation.html",
                "verifyLink", verifyUrl,
                "firstName", bean.firstName);
        FisholaMail mail = builder
                .addTos(email)
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
            final Map<String, String> claims = jwtHelper.verifyCustomToken("register", token);

            Function<String, String> getClaimOrFail = claimName -> {
                String result = claims.get(claimName);
                Preconditions.checkState(StringUtils.isNotEmpty(result), "Claim absent: " + claimName);
                return result;
            };

            Function<String, String> getClaimOrNull = claimName -> {
                String value = claims.get(claimName);
                String result = StringUtils.trimToNull(value);
                return result;
            };

            String email = getClaimOrFail.apply(CLAIM_EMAIL);

            if (log.isInfoEnabled()) {
                log.info(String.format("Email verified, create account for %s", email));
            }

            usersDao.create(
                    getClaimOrFail.apply(CLAIM_FIRST_NAME),
                    getClaimOrNull.apply(CLAIM_LAST_NAME),
                    email,
                    getClaimOrFail.apply(CLAIM_PASSWORD_HASHED)
            );

            // TODO: 22/11/2019 Réponse adaptée
            return Response.ok().build();
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
            Preconditions.checkState(byEmail.isPresent(), "Impossible de trouver l'utilisateur : " + bean.email);
            UUID userId = byEmail.get().getId();

            String token = jwtHelper.createToken(userId);

            NewCookie loginCookie = createTokenCookie(token);
            Response result = Response.ok().cookie(loginCookie).build();
            return result;
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    @POST
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePassword(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, UpdatePasswordBean bean) {

        if (bean == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        UUID userId = getUserId(cookie);

        Optional<FisholaUser> user = usersDao.findById(userId);
        boolean loginResult = user.map(FisholaUser::getEmail)
                .map(email -> usersDao.authenticate(email, bean.currentPassword))
                .map(optional -> optional.orElse(false)) // authent failed
                .orElse(false); // user not found

        Map<String, String> validationErrors = new HashMap<>();

        if (loginResult) {

            if (StringUtils.isEmpty(bean.newPassword)) {
                validationErrors.put("newPassword", "Le mot de passe est obligatoire");
            } else if (bean.newPassword.length() < 6) {
                validationErrors.put("newPassword", "Le mot de passe doit comporter au moins 6 caractères");
            }

        } else {
            validationErrors.put("currentPassword", "Mot de passe erroné");
        }

        if (!validationErrors.isEmpty()) {
            Response response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(validationErrors)
                    .build();
            return response;
        }

        Preconditions.checkState(user.isPresent(), "Si l'utilisateur n'était pas trouvé on aurait failé avant");

        FisholaUser existingUser = user.get();
        String hashedPassword = usersDao.hashPassword(bean.newPassword);
        existingUser.setPassword(hashedPassword);

        usersDao.updateUser(existingUser);

        return Response.ok().build();
    }

    // XXX AThimel 19/02/2020 : Devrait être en POST
    @GET
    @Path("/logout")
    public Response logout(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        // Pour le logout on va générer un cookie qui va écraser/effacer le cookie normal
        NewCookie logoutCookie = dropTokenCookie();
        Response result = Response.noContent().cookie(logoutCookie).build();
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

    @PUT
    @Path("/profile")
    public Response saveProfile(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, UserProfile profile) {
        UUID userId = getUserId(cookie);
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});

        user.setFirstName(profile.firstName());
        user.setLastName(profile.lastName().map(StringUtils::trimToNull).orElse(null));
        user.setEmail(profile.email().toLowerCase());
        user.setBirthYear(profile.birthYear().orElse(null));
        user.setGender(profile.gender().orElse(null));

        Map<String, String> validationErrors = validateProfile(user);

        if (!validationErrors.isEmpty()) {
            Response response = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(validationErrors)
                    .build();
            return response;
        }

        usersDao.updateUser(user);

        return Response.noContent().build();
    }

    protected Map<String, String> validateProfile(FisholaUser bean) {

        Map<String, String> result = new HashMap<>();

        if (StringUtils.isEmpty(bean.getFirstName())) {
            result.put("firstName", "Le prénom est obligatoire");
        }

        if (StringUtils.isEmpty(bean.getEmail())) {
            result.put("email", "L'e-mail est obligatoire");
        } else if (!isEmailInValidFormat(bean.getEmail())) {
            // On vérifie qu'il n'y a pas déjà un compte avec cet email
            result.put("email", "Le format n'est pas correct");
        } else {
            Optional<FisholaUser> existingUser = usersDao.findByEmail(bean.getEmail());
            if (existingUser.isPresent() && !bean.getId().equals(existingUser.get().getId())) {
                // On vérifie qu'il n'y a pas déjà un compte avec cet email
                result.put("email", "E-mail déjà utilisé");
            }
        }

        return result;
    }

    @GET
    @Path("/settings")
    public UserSettings getSettings(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie) {
        UUID userId = getUserId(cookie);
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});

        UserSettings result = ImmutableUserSettings.builder()
                .promptWeight(user.getPromptWeight())
                .promptSamples(user.getPromptSamples())
                .build();
        return result;
    }

    @PUT
    @Path("/settings")
    public Response saveSettings(@CookieParam(AUTHENTICATION_COOKIE_NAME) Cookie cookie, UserSettings settings) {
        UUID userId = getUserId(cookie);
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});

        user.setPromptWeight(settings.promptWeight());
        user.setPromptSamples(settings.promptSamples());

        usersDao.updateUser(user);

        return Response.noContent().build();
    }

}
