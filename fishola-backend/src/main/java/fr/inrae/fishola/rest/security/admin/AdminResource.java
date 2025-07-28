package fr.inrae.fishola.rest.security.admin;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2025 INRAE - UMR CARRTEL
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
import fr.inrae.fishola.database.AdminDao;
import fr.inrae.fishola.entities.tables.pojos.FisholaAdmin;
import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import fr.inrae.fishola.mails.FisholaMail;
import fr.inrae.fishola.mails.ImmutableFisholaMail;
import fr.inrae.fishola.rest.security.AbstractSecurityFisholaResource;
import fr.inrae.fishola.rest.security.AdminProfileForAdmin;
import fr.inrae.fishola.rest.security.ImmutableAdminProfileForAdmin;
import fr.inrae.fishola.rest.security.LoginBean;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jooq.exception.DataAccessException;
import org.nuiton.util.ResourceNotFoundException;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Path("/api/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminResource extends AbstractSecurityFisholaResource {
    protected static final String CLAIM_CAN_CREATE_ADMIN = "canCreateAdmin";
    protected static final String CLAIM_LAKE_IDS = "lakeIds";

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response adminLogin(LoginBean loginBean) {

        Optional<Boolean> authenticate = adminDao.authenticate(loginBean.email, loginBean.password);
        if (authenticate.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (authenticate.get()) {
            Optional<FisholaAdmin> byEmail = adminDao.findByEmail(loginBean.email);
            String token = jwtHelper.createAdminToken(byEmail.get().getId());
            NewCookie adminCookie = createAdminTokenCookie(token);
            Response result = Response.noContent()
                    // Cannot use cookie() method as SameSite is not yet supported in NewCookie (but is planned to be soon)
                    .header("Set-Cookie", adminCookie+";SameSite=None")
                    .build();
            return result;
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(RegisterAdminBean bean, @Context HttpServletRequest request) {
        if (bean == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Map<String, String> validationErrors = new HashMap<>();
        String email = StringUtils.trimToEmpty(bean.email).toLowerCase();
        if (StringUtils.isEmpty(email)) {
            validationErrors.put("email", "L'e-mail est obligatoire");
        } else if (!isEmailInValidFormat(email)) {
            // On vérifie qu'il n'y a pas déjà un compte avec cet email
            validationErrors.put("email", "Le format n'est pas correct");
        } else if (adminDao.findByEmail(email).isPresent()) {
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

        String passwordHashed = adminDao.hashPassword(bean.password);

        Map<String, String> claims = new HashMap<>();
        claims.put(CLAIM_EMAIL, email);
        claims.put(CLAIM_PASSWORD_HASHED, passwordHashed);
        claims.put(CLAIM_LAKE_IDS, bean.lakeIds.stream().map(UUID::toString).collect(Collectors.joining("")));
        claims.put(CLAIM_CAN_CREATE_ADMIN, bean.canCreateAdmin.toString());

        String token = jwtHelper.createCustomToken("register-admin", 1, claims);

        if (config.autoVerifyAccounts()) {
            try {
                verifyAfterRegistrationFromMail(request, token);
            } catch (Exception eee) {
                log.error("Unable to verify token", eee);
            }
        } else {

            String apiBaseUrl = config.getApiUrl("/api/v1/admin/verify", request);
            String verifyUrl = String.format("%s?t=%s", apiBaseUrl, token);

            ImmutableFisholaMail.Builder builder = mailService.newMailFromTemplate(
                    "emails/admin-email-validation.html",
                    "verifyLink", verifyUrl,
                    "email", bean.email);
            FisholaMail mail = builder
                    .addTos(email)
                    .subject("FISHOLA - Validation de votre e-mail")
                    .build();

            mailService.sendMail(mail);
        }

        return Response.ok().build();
    }

    @POST
    @Path("/logout")
    public Response adminLogout() {
        // Pour le logout on va générer un cookie qui va écraser/effacer le cookie normal
        NewCookie logoutCookie = dropAdminTokenCookie();
        Response result = Response.noContent()
                // Cannot use cookie() method as SameSite is not yet supported in NewCookie (but is planned to be soon)
                .header("Set-Cookie", logoutCookie+";SameSite=None")
                .build();
        return result;
    }


    @GET
    @Path("/check")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response adminCheck() throws ResourceNotFoundException {
        FisholaAdmin fisholaAdmin = checkIsAdmin();
        LoggedAdminBean loggedAdmin = new LoggedAdminBean(
                fisholaAdmin.getEmail(),
                fisholaAdmin.getIsNationalAdmin(),
                fisholaAdmin.getCanCreateAdmin()
        );
        return Response.ok(loggedAdmin).build();
    }

    /**
     * Triggered when admins opens the verify mail.
     * Success/failure will be displayed as an html page.
     */
    @GET
    @Path("/verify")
    public Response verifyAfterRegistrationFromMail(@Context HttpServletRequest request, @QueryParam("t") String token) {
        if (doVerifyAfterRegistration(request, token)) {
            String verifiedUrl = config.getApiUrl("/api/admin_verify_ok.html", request);
            Response success = Response.temporaryRedirect(URI.create(verifiedUrl)).build();
            return success;
        } else {
            String verifiedUrl = config.getApiUrl("/api/admin_verify_fail.html", request);
            Response error = Response.temporaryRedirect(URI.create(verifiedUrl)).build();
            return error;
        }
    }

    protected AdminProfileForAdmin toUserProfileForAdmin(FisholaAdmin input) {
        ImmutableAdminProfileForAdmin result = ImmutableAdminProfileForAdmin.builder()
                .id(input.getId())
                .email(input.getEmail())
                .canCreateAdmin(input.getCanCreateAdmin())
                .isNationalAdmin(input.getIsNationalAdmin())
                .build();
        return result;
    }

    @GET
    @Path("/")
    public List<AdminProfileForAdmin> listAdmins() {
        FisholaAdmin fisholaAdmin = checkIsAdmin();
        List<FisholaAdmin> admins = adminDao.findAll();
        Set<UUID> allowedLakes = getAllowedAdminLakes();
        List<AdminProfileForAdmin> result = admins.stream()
            .filter(admin -> {
                if (fisholaAdmin.getIsNationalAdmin()) {
                    return true;
                }
                // Local admins can only see admin of their lakes
                Set<UUID> adminLakes = adminDao.getAllowedLakes(admin.getId());
                return !Collections.disjoint(allowedLakes, adminLakes);
            })
            .map(this::toUserProfileForAdmin)
            .toList();
        return result;
    }

    private boolean doVerifyAfterRegistration(@Context HttpServletRequest request, @QueryParam("t") String token) {
        try {
            final Map<String, String> claims = jwtHelper.verifyCustomToken("register-admin", token);

            UnaryOperator<String> getClaimOrFail = claimName -> {
                String result = claims.get(claimName);
                Preconditions.checkState(StringUtils.isNotEmpty(result), "Claim absent: %s".formatted(claimName));
                return result;
            };

            String email = getClaimOrFail.apply(CLAIM_EMAIL);

            if (log.isInfoEnabled()) {
                log.infof("Email verified, create account for %s", email);
            }

            adminDao.create(
                    email,
                    getClaimOrFail.apply(CLAIM_PASSWORD_HASHED),
                    Boolean.parseBoolean(getClaimOrFail.apply(CLAIM_CAN_CREATE_ADMIN)),
                    false
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
}
