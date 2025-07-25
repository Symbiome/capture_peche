package fr.inrae.fishola.rest.security;

import fr.inrae.fishola.mails.MailService;
import fr.inrae.fishola.rest.AbstractFisholaResource;
import jakarta.inject.Inject;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.util.Optional;

public class AbstractSecurityFisholaResource  extends AbstractFisholaResource {
    protected static final String CLAIM_EMAIL = "email";
    protected static final String CLAIM_PASSWORD_HASHED = "passwordHashed";

    @Inject
    protected MailService mailService;

    protected Optional<String> validatePassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return Optional.of("Le mot de passe est obligatoire");
        } else if (password.length() < 6) {
            return Optional.of("Le mot de passe doit comporter au moins 6 caractères");
        }
        return Optional.empty();
    }

    protected boolean isEmailInValidFormat(String email) {
        try {
            InternetAddress internetAddress = new InternetAddress(email);
            internetAddress.validate();
            return true;
        } catch (AddressException ex) {
            if (log.isInfoEnabled()) {
                log.infof("'%s' does not seem to be a valid email address", email);
            }
            return false;
        }
    }
}
