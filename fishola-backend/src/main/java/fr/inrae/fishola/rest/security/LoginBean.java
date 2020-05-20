package fr.inrae.fishola.rest.security;

public class LoginBean {

    public String email;
    public String password;

    public LoginBean() {
    }

    public LoginBean(String email, String password) {
        this.email = email;
        this.password = password;
    }

}
