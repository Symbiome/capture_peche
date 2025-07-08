package fr.inrae.fishola.rest.security.admin;

public class LoggedAdminBean {
    public String email;
    public Boolean isNationalAdmin;
    public final Boolean canCreateAdmins;

    public LoggedAdminBean(String email, Boolean isNationalAdmin, Boolean canCreateAdmins) {
        this.email = email;
        this.isNationalAdmin = isNationalAdmin;
        this.canCreateAdmins = canCreateAdmins;
    }
}
