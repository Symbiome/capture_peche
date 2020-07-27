package fr.inrae.fishola.rest.security;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.google.common.base.Preconditions;
import fr.inrae.fishola.database.TripsDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaUser;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import fr.inrae.fishola.exceptions.NotAuthenticatedException;
import fr.inrae.fishola.exceptions.NotFoundException;
import fr.inrae.fishola.mails.FisholaMail;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import fr.inrae.fishola.rest.UserIdAndRenewal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.exception.DataAccessException;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/api/v1/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource extends AbstractFisholaResource {

    private static final Log log = LogFactory.getLog(SecurityResource.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_FIRST_NAME = "firstName";
    private static final String CLAIM_LAST_NAME = "lastName";
    private static final String CLAIM_PASSWORD_HASHED = "passwordHashed";

    @Inject
    protected MailService mailService;

    @Inject
    protected TripsDao tripsDao;

    protected Optional<String> validatePassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return Optional.of("Le mot de passe est obligatoire");
        } else if (password.length() < 6) {
            return Optional.of("Le mot de passe doit comporter au moins 6 caractères");
        }
        return Optional.empty();
    }

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

        Optional<String> passwordError = validatePassword(bean.password);
        passwordError.ifPresent(error -> validationErrors.put("password", error));

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

        if (config.isAutoVerifyAccounts()) {
            try {
                verifyAfterRegistrationFromMail(request, token);
            } catch (Exception eee) {
                log.error("Unable to verify token", eee);
            }
        } else {

            String apiBaseUrl = config.getApiUrl("/api/v1/security/verify", request);
            String verifyUrl = String.format("%s?t=%s", apiBaseUrl, token);

            ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                    "emails/email-validation.html",
                    "verifyLink", verifyUrl,
                    "firstName", bean.firstName);
            FisholaMail mail = builder
                    .addTos(email)
                    .subject("FISHOLA - Validation de votre e-mail")
                    .build();

            mailService.sendMail(mail);
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

    /**
     * Triggered when users opens the verify mail and does not have app installed.
     * Success/failure will be displayed as an html page.
     */
    @GET
    @Path("/verify")
    public Response verifyAfterRegistrationFromMail(@Context HttpServletRequest request, @QueryParam("t") String token) {
        if (doVerifyAfterRegistration(request, token)) {
            String verifiedUrl = config.getApiUrl("/api/verify_ok.html", request);
            Response success = Response.temporaryRedirect(URI.create(verifiedUrl)).build();
            return success;
        } else {
            String verifiedUrl = config.getApiUrl("/api/verify_fail.html", request);
            Response error = Response.temporaryRedirect(URI.create(verifiedUrl)).build();
            return error;
        }
    }
    /**
     * Triggered when users opens the verify mail and does have app installed.
     * Success/failure will be displayed as an HTTP response code
     */
    @GET
    @Path("/verify-app")
    public Response verifyAfterRegistrationFromApp(@Context HttpServletRequest request, @QueryParam("t") String token) {
        if (doVerifyAfterRegistration(request, token)) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    private boolean doVerifyAfterRegistration(@Context HttpServletRequest request, @QueryParam("t") String token) {
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

            return true;
        } catch (DataAccessException dae) {
            // Cannot access DB: Silent catch, reset fail page will be shown
            log.warn("Password reset fail : DataAccesException", dae);
        }  catch (FisholaTechnicalException fte) {
            // Token may have expire: silent catch, reset fail page will be shown
            log.warn("Password reset fail : FisholaTechnicalException", fte);
        }  catch (JWTDecodeException jde) {
            // Token may have expire: silent catch, reset fail page will be shown
            log.warn("Password reset fail : JWTDecodeException", jde);
        }
        return false;
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

            String token = jwtHelper.createUserToken(userId);

            NewCookie loginCookie = createUserTokenCookie(token);
            Response result = Response.ok().cookie(loginCookie).build();
            return result;
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    /**
     * Password reset request: sends an email allowing user to modify his password.
     * @param reset the email on which password should be reset and the new desired password
     * @return 200 if password request mail was sent, 404 if mail not found
     */
    @POST
    @Path("/request-password-reset")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response requestPasswordReset(LoginBean reset, @Context HttpServletRequest request) {
        Optional<FisholaUser> correspondingUser = usersDao.findByEmail(reset.email);
        if (correspondingUser.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            // Step 1: check that user with given email exists
            // NB : ensured by the test above

            // Step 2: validate & hash password
            Optional<String> passwordError = validatePassword(reset.password);
            Map<String, String> validationErrors = new LinkedHashMap<>();
            passwordError.ifPresent(error -> validationErrors.put("password", error));
            if (!validationErrors.isEmpty()) {
                Response response = Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(validationErrors)
                        .build();
                return response;
            }
            String passwordHashed = usersDao.hashPassword(reset.password);

            // Step 3: Prepare a special token that will allow user to reset password once clicked from emails
            Map<String, String> claims = new HashMap<>();
            claims.put(CLAIM_EMAIL, reset.email);
            claims.put(CLAIM_PASSWORD_HASHED, passwordHashed);
            String token = jwtHelper.createCustomToken("reset-password", 1, claims);
            String apiBaseUrl = config.getApiUrl("/api/v1/security/reset-password", request);
            String resetUrl = String.format("%s?t=%s", apiBaseUrl, token);

            // Step 4: send mail
            ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                    "emails/password_reset.html",
                    "resetLink", resetUrl,
                    "firstName", correspondingUser.get().getFirstName());
            FisholaMail mail = builder
                    .addTos(reset.email)
                    .subject("FISHOLA - Réinitialisation de votre mot de passe")
                    .build();

            mailService.sendMail(mail);
            return Response.ok().build();
        }
    }

    /**
     * Triggered when users opens the reset mail and does not have app installed.
     * Success/failure will be displayed as an html page.
     */
    @GET
    @Path("/reset-password")
    public Response resetPasswordFromMail(@Context HttpServletRequest request, @QueryParam("t") String token) {
        if (doResetPassword(request, token)) {
            String verifiedUrl = config.getApiUrl("/api/password_reset_ok.html", request);
            Response success = Response.temporaryRedirect(URI.create(verifiedUrl)).build();
            return success;
        } else {
            String verifiedUrl = config.getApiUrl("/api/password_reset_fail.html", request);
            Response error = Response.temporaryRedirect(URI.create(verifiedUrl)).build();
            return error;
        }
    }
    /**
     * Triggered when users opens the reset mail and does have app installed.
     * Success/failure will be displayed as an HTTP response code
     */
    @GET
    @Path("/reset-password-app")
    public Response resetPasswordFromApp(@Context HttpServletRequest request, @QueryParam("t") String token) {
        if (doResetPassword(request, token)) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    private Boolean doResetPassword(HttpServletRequest request, String token) {
        try {
            final Map<String, String> claims = jwtHelper.verifyCustomToken("reset-password", token);
            Function<String, String> getClaimOrFail = claimName -> {
                String result = claims.get(claimName);
                Preconditions.checkState(StringUtils.isNotEmpty(result), "Claim absent: " + claimName);
                return result;
            };
            String email = getClaimOrFail.apply(CLAIM_EMAIL);
            String newPasswordHashed = getClaimOrFail.apply(CLAIM_PASSWORD_HASHED);
            if (log.isInfoEnabled()) {
                log.info(String.format("Password reset for %s", email));
            }
            Optional<FisholaUser> user = usersDao.findByEmail(email);
            if (user.isPresent()) {
                FisholaUser existingUser = user.get();
                existingUser.setPassword(newPasswordHashed);
                usersDao.updateUser(existingUser);
                return true;
            }
        } catch (DataAccessException dae) {
            // Cannot access DB: Silent catch, reset fail page will be shown
            log.warn("Password reset fail : DataAccesException", dae);
        }  catch (FisholaTechnicalException fte) {
            // Token may have expire: silent catch, reset fail page will be shown
            log.warn("Password reset fail : FisholaTechnicalException", fte);
        } catch (JWTDecodeException jde) {
            // Token may have expire: silent catch, reset fail page will be shown
            log.warn("Password reset fail : JWTDecodeException", jde);
        }
        return false;
    }

    @POST
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePassword(UpdatePasswordBean bean) {

        if (bean == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();

        Optional<FisholaUser> user = usersDao.findById(userId);
        boolean loginResult = user.map(FisholaUser::getEmail)
                .map(email -> usersDao.authenticate(email, bean.currentPassword))
                .map(optional -> optional.orElse(false)) // authent failed
                .orElse(false); // user not found

        Map<String, String> validationErrors = new HashMap<>();

        if (loginResult) {

            Optional<String> passwordError = validatePassword(bean.newPassword);
            passwordError.ifPresent(error -> validationErrors.put("newPassword", error));

        } else {
            validationErrors.put("currentPassword", "Mot de passe erroné");
        }

        if (!validationErrors.isEmpty()) {
            Response.ResponseBuilder builder = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(validationErrors);
            Response response = buildResponse(builder, userIdAndRenewal);
            return response;
        }

        Preconditions.checkState(user.isPresent(), "Si l'utilisateur n'était pas trouvé on aurait failé avant");

        FisholaUser existingUser = user.get();
        String hashedPassword = usersDao.hashPassword(bean.newPassword);
        existingUser.setPassword(hashedPassword);

        usersDao.updateUser(existingUser);

        Response result = noContent(userIdAndRenewal);
        return result;
    }

    @POST
    @Path("/logout")
    public Response logout() {
        // Pour le logout on va générer un cookie qui va écraser/effacer le cookie normal
        NewCookie logoutCookie = dropUserTokenCookie();
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
                .sampleBaseId(encodeSampleBaseId(input.getSampleBaseId()))
                .build();
        return result;
    }

    public static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static String encodeSampleBaseId(final int rawNumber) {
        int low = rawNumber % ALPHABET.length;
        String result = String.valueOf(ALPHABET[low]);
        int remaining = rawNumber - ALPHABET.length;
        if (remaining >= 0) {
            int high = remaining / ALPHABET.length;
            result = encodeSampleBaseId(high) + result;
        }
        return result;
    }

    @GET
    @Path("/profile")
    public Response getProfile() {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});
        UserProfile result = toUserProfile(user);
        Response response = wrapEntity(result, userIdAndRenewal);
        return response;
    }

    @PUT
    @Path("/profile")
    public Response saveProfile(UserProfile profile) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});

        user.setFirstName(profile.firstName());
        user.setLastName(profile.lastName().map(StringUtils::trimToNull).orElse(null));
        user.setEmail(profile.email().toLowerCase());
        user.setBirthYear(profile.birthYear().orElse(null));
        user.setGender(profile.gender().orElse(null));

        Map<String, String> validationErrors = validateProfile(user);

        if (!validationErrors.isEmpty()) {
            Response.ResponseBuilder responseBuilder = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(validationErrors);
            Response response = buildResponse(responseBuilder, userIdAndRenewal);
            return response;
        }

        usersDao.updateUser(user);

        Response response = noContent(userIdAndRenewal);
        return response;
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
    public Response getSettings() {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});

        UserSettings result = ImmutableUserSettings.builder()
                .promptWeight(user.getPromptWeight())
                .promptSamples(user.getPromptSamples())
                .build();
        Response response = wrapEntity(result, userIdAndRenewal);
        return response;
    }

    @PUT
    @Path("/settings")
    public Response saveSettings( UserSettings settings) {
        UserIdAndRenewal userIdAndRenewal = getUserIdOrRenew();
        UUID userId = userIdAndRenewal.userId();
        Optional<FisholaUser> optional = usersDao.findById(userId);
        FisholaUser user = optional.orElseThrow(() -> {throw new NotAuthenticatedException("Utilisateur inconnu");});

        user.setPromptWeight(settings.promptWeight());
        user.setPromptSamples(settings.promptSamples());

        usersDao.updateUser(user);

        Response response = noContent(userIdAndRenewal);
        return response;
    }

    protected UserProfileForAdmin toUserProfileForAdmin(FisholaUser input) {
        ImmutableUserProfileForAdmin result = ImmutableUserProfileForAdmin.builder()
                .id(input.getId())
                .email(input.getEmail())
                .firstName(input.getFirstName())
                .lastName(Optional.ofNullable(input.getLastName()))
                .birthYear(Optional.ofNullable(input.getBirthYear()))
                .gender(Optional.ofNullable(input.getGender()))
                .excludeFromExports(input.getExcludeFromExports())
                .build();
        return result;
    }

    @GET
    @Path("/users")
    public List<UserProfileForAdmin> listUsers() {
        checkIsAdmin();
        // TODO AThimel 07/07/2020 Pagination
        List<FisholaUser> users = usersDao.findAll();
        List<UserProfileForAdmin> result = users.stream()
                .map(this::toUserProfileForAdmin)
                .collect(Collectors.toList());
        return result;
    }

    @PUT
    @Path("/users/{userId}")
    public Response updateUser(@PathParam("userId") UUID userId, UserProfileForAdmin user) {
        checkIsAdmin();
        Preconditions.checkArgument(userId.equals(user.id()), "L'identifiant ne correspond pas");
        Optional<FisholaUser> optional = usersDao.findById(userId);
        NotFoundException.check(optional.isPresent(), "L'utilisateur n'existe pas : " + userId);
        FisholaUser existingUser = optional.get();
        // On ne met volontairement pas les autres champs à jour car c'est juste pour la partie admin
        existingUser.setExcludeFromExports(user.excludeFromExports());
        usersDao.updateUser(existingUser);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/users/{userId}")
    public Response deleteUser(@PathParam("userId") UUID userId) {
        checkIsAdmin();
        Optional<FisholaUser> optional = usersDao.findById(userId);
        NotFoundException.check(optional.isPresent(), "L'utilisateur n'existe pas : " + userId);
        FisholaUser existingUser = optional.get();
        tripsDao.unsetOwner(userId);
        usersDao.deleteUser(existingUser);
        return Response.noContent().build();
    }

    @POST
    @Path("/admin-login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response adminLogin(LoginBean loginBean) {

        if (config.getAdminPassword().equals(loginBean.password)) {

            String token = jwtHelper.createAdminToken();

            NewCookie loginCookie = createAdminTokenCookie(token);
            Response result = Response.noContent().cookie(loginCookie).build();
            return result;
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

    }

    @GET
    @Path("/admin-check")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response adminCheck() {
        checkIsAdmin();
        Response result = Response.noContent().build();
        return result;
    }

    @POST
    @Path("/admin-logout")
    public Response adminLogout() {
        // Pour le logout on va générer un cookie qui va écraser/effacer le cookie normal
        NewCookie logoutCookie = dropAdminTokenCookie();
        Response result = Response.noContent().cookie(logoutCookie).build();
        return result;
    }

}
