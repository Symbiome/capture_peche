package fr.inra.fishola.rest;

import fr.inra.fishola.database.UsersDao;
import fr.inra.fishola.exceptions.NotAuthenticatedException;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.StreamingOutput;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static javax.transaction.Transactional.TxType.REQUIRED;
import static javax.ws.rs.core.Cookie.DEFAULT_VERSION;
import static javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

@Transactional(REQUIRED)
public abstract class AbstractFisholaResource {

    protected static final String AUTHENTICATION_COOKIE_NAME = "token";

    @Inject
    protected JwtHelper jwtHelper;

    @Inject
    protected UsersDao usersDao;

    protected NewCookie createTokenCookie(String token) {
        NewCookie result = newTokenCookie(token, DEFAULT_MAX_AGE);
        return result;
    }

    protected NewCookie dropTokenCookie() {
        NewCookie result = newTokenCookie("", 0);
        return result;
    }

    private NewCookie newTokenCookie(String token, int maxAge) {
        // FIXME AThimel 21/11/2019 Secure + HTTPOnly
        NewCookie result = new NewCookie(
                AUTHENTICATION_COOKIE_NAME,
                token,
                "/api",
                null,
                DEFAULT_VERSION,
                null,
                maxAge,
                null,
                false,
                true);
        return result;
    }

    protected UUID getUserId(Cookie cookie) {
        String token = Optional.ofNullable(cookie)
                .map(Cookie::getValue)
                .orElse(null);
        UUID userId = jwtHelper.tokenToUserID(token);
        // Now we have to check that the userId is valid
        if (!usersDao.isValidUserId(userId)) {
            throw new NotAuthenticatedException("Utilisateur inconnu");
        }
        return userId;
    }

    protected StreamingOutput wrapAsStreamingOutput(byte[] array) {
        return output -> {
            output.write(array);
            output.flush();
        };
    }

    // FIXME AThimel 23/03/2020 À dégager et utiliser LocalDate uniquement
    @Deprecated
    protected static Date localDateToDate(LocalDate input) {
        if (input == null) {
            return null;
        }
        ZoneId zoneId = ZoneId.systemDefault();
        Instant instant = input.atStartOfDay(zoneId).toInstant();
        Date result = Date.from(instant);
        return result;
    }

    // FIXME AThimel 23/03/2020 À dégager et utiliser LocalTime uniquement
    @Deprecated
    protected static Date localTimeToDate(LocalDate date, LocalTime time) {
        if (time == null) {
            return null;
        }
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime input = time.atDate(date);
        Instant instant = input.atZone(zoneId).toInstant();
        Date result = Date.from(instant);
        return result;
    }

}
