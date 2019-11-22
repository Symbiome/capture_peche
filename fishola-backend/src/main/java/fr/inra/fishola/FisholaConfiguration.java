package fr.inra.fishola;

import javax.inject.Singleton;

@Singleton
public class FisholaConfiguration {

    public String getJdbcUrl() {
        return "jdbc:postgresql://172.17.0.2/fishola";
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
        return "http://192.168.99.107:8080";
    }

}
