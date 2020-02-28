package fr.inra.fishola;

import fr.inra.fishola.exceptions.FisholaTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Properties;

@Singleton
public class FisholaConfiguration {

    private static final Log log = LogFactory.getLog(FisholaConfiguration.class);

    public int getPasswordHashCost() {
        return 12;
    }

    public String getJwtSecret() {
        return "v3ry 53cr37 k3y";
    }

    public String getBackendBaseUrl() {
        return null; // "https://fishola.demo.codelutin.com"
    }

    public int getTripModifiableHours() {
//        return 7 * 24; // 7 jours
        return 24; // 1 jour
//        return 5; // 5 heures
    }

    public String getBackendBaseUrl(HttpServletRequest httpServletRequest) {
        String backendBaseUrl = getBackendBaseUrl();
        if (StringUtils.isEmpty(backendBaseUrl)) {
            String requestUrl = httpServletRequest.getRequestURL().toString();
            String requestUri = httpServletRequest.getRequestURI();
            int index = requestUrl.indexOf(requestUri);
            if (index != -1) {
                backendBaseUrl = requestUrl.substring(0, index);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("No 'BackendBaseUrl' in conf, computed is: %s", backendBaseUrl));
                }
            } else {
                throw new FisholaTechnicalException("Unable to compute backendBaseUrl", null);
            }
        }
        return backendBaseUrl;
    }

    public String getApiUrl(String path, HttpServletRequest httpServletRequest) {
        return String.format("%s%s", getBackendBaseUrl(httpServletRequest), path);
    }

    public String getMailFrom() {
        return "fishola@codelutin.com";
    }

    public Properties getMailProperties() {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", false);
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", "docker_mail");
        prop.put("mail.smtp.port", "25");
//        prop.put("mail.smtp.ssl.trust", "smtp.mailtrap.io");
        return prop;
    }

    public float getRawImageQuality() {
        // Par défaut la qualité semble être de ~0.74.
        // Pour une qualité équivalent mettre 0.98.
        // .90 pourrait être un bon compromis.

//        return .98f;
        return .90f;
    }

    public File getPicturesPreviewFolder() {
        File result = new File("/tmp/fishola-pictures");
        if (result.mkdirs() && log.isInfoEnabled()) {
            log.info("Création du dossier de stockage des previews : " + result.getAbsolutePath());
        }
        return result;
    }

}
