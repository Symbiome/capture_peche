package fr.inrae.fishola;

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

import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.runtime.configuration.ProfileManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Optional;
import java.util.Properties;

@ConfigProperties(prefix = "fishola")
public interface FisholaConfiguration {

    String getVersion();

    String getGitRevision();

    default String getFullVersion() {
        String result = String.format("%s (%s)", getVersion(), getGitRevision());
        return result;
    }

    String getBuildDate();

    default String getActiveProfile() {
        return ProfileManager.getActiveProfile();
    }

    default boolean isDevMode() {
        String activeProfile = this.getActiveProfile();
        boolean result = "dev".equals(activeProfile);
        return result;
    }

    @ConfigProperty(defaultValue = "12")
    int getPasswordHashCost();

    String getJwtSecret();

    @ConfigProperty(defaultValue = "24")
    int getJwtLifetimeHours();

    @ConfigProperty(defaultValue = "168") // Renouvellement accepté pendant 7j (168=7*24) après expiration
    int getJwtRenewalHours();

    @ConfigProperty(defaultValue = "168")
    int getTripModifiableHours();

    @ConfigProperty(defaultValue = "168") // Délai avant que les sorties soient disponibles dans le fichier d'export. En théorie ce chiffre doit être le même que #getTripModifiableHours()
    int getExportSafeHours();

    Optional<String> getBackendBaseUrl();

    default String computeBackendBaseUrl(HttpServletRequest httpServletRequest) {
        Optional<String> backendBaseUrl = getBackendBaseUrl();
        String result;
        if (backendBaseUrl.isPresent()) { // Ne pas utiliser de lambda sinon ça plante au démarrage
            result = backendBaseUrl.get();
        } else {
            String requestUrl = httpServletRequest.getRequestURL().toString();
            String requestUri = httpServletRequest.getRequestURI();
            int index = requestUrl.indexOf(requestUri);
            if (index != -1) {
                result = requestUrl.substring(0, index);
                Log log = LogFactory.getLog(FisholaConfiguration.class);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("No 'BackendBaseUrl' in conf, computed is: %s", result));
                }
            } else {
                throw new FisholaTechnicalException("Unable to compute backendBaseUrl", null);
            }
        }
        return result;
    }

    default String getApiUrl(String path, HttpServletRequest httpServletRequest) {
        String backendBaseUrl = computeBackendBaseUrl(httpServletRequest);
        String result = String.format("%s%s", backendBaseUrl, path);
        return result;
    }

    @ConfigProperty(defaultValue = "fishola@codelutin.com")
    String getMailFrom();

    Optional<String> getSmtpUsername();

    Optional<String> getSmtpPassword();

    @ConfigProperty(defaultValue = "false")
    boolean getSmtpStarttls();

    String getSmtpHost();

    @ConfigProperty(defaultValue = "25")
    int getSmtpPort();

    default Properties getMailProperties() {
        boolean auth = getSmtpUsername().isPresent() && getSmtpPassword().isPresent();
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", auth);
        prop.put("mail.smtp.starttls.enable", String.valueOf(getSmtpStarttls()));
        prop.put("mail.smtp.host", getSmtpHost());
        prop.put("mail.smtp.port", String.valueOf(getSmtpPort()));
//        prop.put("mail.smtp.ssl.trust", "smtp.mailtrap.io");
        return prop;
    }

    @ConfigProperty(defaultValue = "true")
    boolean isAsyncEmails();

    @ConfigProperty(defaultValue = "60")
    int getAsyncEmailsRetentionMinutes();

    /**
     * Par défaut la qualité semble être de ~0.74.
     * Pour une qualité équivalent mettre 0.98.
     * .90 pourrait être un bon compromis.
     */
    @ConfigProperty(defaultValue = ".90f")
    float getRawImageQuality();

    @ConfigProperty(defaultValue = "/tmp/fishola-pictures")
    String getPicturesPreviewFolderPath();

    default File getPicturesPreviewFolder() {
        File result = new File(getPicturesPreviewFolderPath());
        Log log = LogFactory.getLog(FisholaConfiguration.class);
        if (result.mkdirs() && log.isInfoEnabled()) {
            log.info("Création du dossier de stockage des previews : " + result.getAbsolutePath());
        }
        return result;
    }

    String getFeedbackMailTo();

    @ConfigProperty(defaultValue = "false")
    boolean isAutoVerifyAccounts();

    @ConfigProperty(defaultValue = "53cr37")
    String getAdminPassword();

    @ConfigProperty(defaultValue = "24")
    long getKeyFiguresTimeoutHours();

    @ConfigProperty(defaultValue = "true")
    boolean isDashboardOnlyCurrentYear();

}
