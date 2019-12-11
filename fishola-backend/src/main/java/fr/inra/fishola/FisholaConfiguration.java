package fr.inra.fishola;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

@Singleton
public class FisholaConfiguration {

    public String getJdbcUrl() {
        return "jdbc:postgresql://docker_pg/fishola";
    }

    public String getJdbcUser() {
        return  "postgres";
    }

    public String getJdbcPassword() {
        return null;
    }

    public int getPasswordHashCost() {
        return 12;
    }

    public String getJwtSecret() {
        return "v3ry 53cr37 k3y";
    }

    public String getBackendBaseUrl() {
        return null; // "https://fishola-backend.demo.codelutin.com"
    }

    public String getBackendBaseUrl(HttpServletRequest httpServletRequest) {
        String backendBaseUrl = getBackendBaseUrl();
        if (StringUtils.isEmpty(backendBaseUrl)) {
            String requestUrl = httpServletRequest.getRequestURL().toString();
            System.out.println("getRequestURL:" + requestUrl);
            String requestUri = httpServletRequest.getRequestURI();
            System.out.println("getRequestURI:" + requestUri);
            int index = requestUrl.indexOf(requestUri);
            if (index != -1) {
                backendBaseUrl = requestUrl.substring(0, index);
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
}
