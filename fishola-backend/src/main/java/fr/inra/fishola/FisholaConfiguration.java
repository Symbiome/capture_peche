package fr.inra.fishola;

import javax.inject.Singleton;
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
        return "https://fishola-backend.demo.codelutin.com";
    }

    public String getApiUrl(String path) {
        return String.format("%s%s", getBackendBaseUrl(), path);
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
