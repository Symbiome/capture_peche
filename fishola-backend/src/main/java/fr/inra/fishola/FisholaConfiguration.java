package fr.inra.fishola;

import fr.inra.fishola.exceptions.FisholaTechnicalException;
import io.quarkus.arc.config.ConfigProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Optional;
import java.util.Properties;

@ConfigProperties(prefix = "fishola")
public interface FisholaConfiguration {

    @ConfigProperty(defaultValue = "12")
    int getPasswordHashCost();

    String getJwtSecret();

    @ConfigProperty(defaultValue = "148")
    int getTripModifiableHours();

    Optional<String> getBackendBaseUrl();

    default String getBackendBaseUrl(HttpServletRequest httpServletRequest) {
        Optional<String> backendBaseUrl = getBackendBaseUrl();
        String result;
        if (backendBaseUrl.isPresent()) { // Ne pas utiliser de lambda sinon ça plante au démarrage
            result = backendBaseUrl.get();
        } else {
            result = computeBackendBaseUrl(httpServletRequest);
        }
        return result;
    }

    default String computeBackendBaseUrl(HttpServletRequest httpServletRequest) {
        String requestUrl = httpServletRequest.getRequestURL().toString();
        String requestUri = httpServletRequest.getRequestURI();
        int index = requestUrl.indexOf(requestUri);
        if (index != -1) {
            String guessed = requestUrl.substring(0, index);
            Log log = LogFactory.getLog(FisholaConfiguration.class);
            if (log.isDebugEnabled()) {
                log.debug(String.format("No 'BackendBaseUrl' in conf, computed is: %s", guessed));
            }
            return guessed;
        } else {
            throw new FisholaTechnicalException("Unable to compute backendBaseUrl", null);
        }
    }

    default String getApiUrl(String path, HttpServletRequest httpServletRequest) {
        String backendBaseUrl = getBackendBaseUrl(httpServletRequest);
        String result = String.format("%s%s", backendBaseUrl, path);
        return result;
    }

    @ConfigProperty(defaultValue = "fishola@codelutin.com")
    String getMailFrom();

    default Properties getMailProperties() {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", false);
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", "docker_mail");
        prop.put("mail.smtp.port", "25");
//        prop.put("mail.smtp.ssl.trust", "smtp.mailtrap.io");
        return prop;
    }

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

}
