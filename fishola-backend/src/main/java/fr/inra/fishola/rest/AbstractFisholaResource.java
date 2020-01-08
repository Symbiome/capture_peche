package fr.inra.fishola.rest;

import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractFisholaResource {

    protected static final String AUTHENTICATION_COOKIE_NAME = "token";

    @Inject
    protected JwtHelper jwtHelper;

    protected UUID getUserId(Cookie cookie) {
        String token = Optional.ofNullable(cookie)
                .map(Cookie::getValue)
                .orElse(null);
        UUID result = jwtHelper.getUserId(token);
        return result;
    }

}
