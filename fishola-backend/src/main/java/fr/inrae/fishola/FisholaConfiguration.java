package fr.inrae.fishola;

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

import fr.inrae.fishola.exceptions.FisholaTechnicalException;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithDefault;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
@ConfigMapping(prefix = "fishola")
public interface FisholaConfiguration {

    String version();

    String gitRevision();

    default String getFullVersion() {
        String result = String.format("%s (%s)", version(), gitRevision());
        return result;
    }

    String buildDate();

    default String getActiveProfile() {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getProfiles().getFirst();
    }

    default boolean isDevMode() {
        String activeProfile = this.getActiveProfile();
        boolean result = "dev".equals(activeProfile);
        return result;
    }

    @WithDefault("12")
    int passwordHashCost();

    String jwtSecret();

    @WithDefault("24")
    int jwtLifetimeHours();

    @WithDefault("168") // Renouvellement accepté pendant 7j (168=7*24) après expiration
    int jwtRenewalHours();

    @WithDefault("168")
    int tripModifiableHours();

    @WithDefault("168") // Délai avant que les sorties soient disponibles dans le fichier d'export. En théorie ce chiffre doit être le même que #getTripModifiableHours()
    int exportSafeHours();

    Optional<String> backendBaseUrl();

    Optional<String> backendDeeplinkSafeBaseUrl();

    // Délai en heure à attendre entre deux envois de notification d'actualité
    // Au moment du check, un unique courriel regroupant toutes les nouvelles actualités publiques est envoyé
    @WithDefault("168")
    int newsMailSendingDelayHours();

    default String computeBackendBaseUrl(HttpServletRequest httpServletRequest) {
        Optional<String> backendBaseUrl = backendBaseUrl();
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

    default String getDeeplinkSafeApiUrl(String path, HttpServletRequest httpServletRequest) {
        Optional<String> deeplinkSafeBaseUrl = backendDeeplinkSafeBaseUrl();
        if (deeplinkSafeBaseUrl.isPresent()) {
            String result = String.format("%s%s", deeplinkSafeBaseUrl.get(), path);
            return result;
        } else {
            return getApiUrl(path, httpServletRequest);
        }
    }

    @WithDefault("true")
    boolean asyncEmails();

    @WithDefault("60")
    int asyncEmailsRetentionMinutes();

    @WithDefault("30s")
    // Utilisé directement par MailService#sendPendingEmails()
    String asyncEmailsEvery();

    /**
     * Par défaut la qualité semble être de ~0.74.
     * Pour une qualité équivalent mettre 0.98.
     * .90 pourrait être un bon compromis.
     */
    @WithDefault(".90f")
    float rawImageQuality();

    @WithDefault("/tmp/fishola-pictures")
    String picturesPreviewFolderPath();

    default File getPicturesPreviewFolder() {
        File result = new File(picturesPreviewFolderPath());
        Log log = LogFactory.getLog(FisholaConfiguration.class);
        if (result.mkdirs() && log.isInfoEnabled()) {
            log.info("Création du dossier de stockage des previews : " + result.getAbsolutePath());
        }
        return result;
    }

    String feedbackMailTo();

    @WithDefault("false")
    boolean autoVerifyAccounts();

    @ConfigProperty
    String adminPassword();

    @WithDefault("24")
    long keyFiguresTimeoutHours();

    @WithDefault("true")
    boolean dashboardOnlyCurrentYear();

    @WithDefault("15")
    long globalDashboardTimeoutMinutes();

    @WithDefault("400")
    long globalDashboardCacheSize();
}
