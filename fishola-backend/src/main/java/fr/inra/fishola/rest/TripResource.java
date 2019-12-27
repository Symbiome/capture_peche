package fr.inra.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fr.inra.fishola.FisholaConfiguration;
import fr.inra.fishola.database.TripDao;
import fr.inra.fishola.database.UserDao;
import fr.inra.fishola.entities.tables.pojos.FisholaUser;
import fr.inra.fishola.entities.tables.pojos.Trip;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Path("/api/v1/trips")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TripResource {

    @Inject
    protected FisholaConfiguration config;

    @Inject
    protected UserDao userDao;

    @Inject
    protected TripDao tripDao;

    @GET
    @Path("/")
    public List<TripLight> getMyTrips() {
        ImmutableTripLight aTrip = ImmutableTripLight.builder()
                .catchs(12)
                .date(new Date())
                .id(UUID.randomUUID().toString())
                .lakeName("Toto")
                .name("azerty")
                .canBeModified(false)
                .build();
        LinkedList<TripLight> result = new LinkedList<>();
        result.add(aTrip);
        return result;
    }

    protected Algorithm getJwtSecretAlgorithm() {
        String jwtSecret = config.getJwtSecret();
        Algorithm result = Algorithm.HMAC512(jwtSecret);
        return result;
    }

    @PUT
    @Path("/")
    public void createTrip(@CookieParam("token") Cookie cookie, TripBean trip) {

        UUID userId = getUserId(cookie);
//        UUID userId = null;

        System.out.println(trip);
        Trip pojo = new Trip();
        pojo.setDay(new java.sql.Date(trip.date.getTime()));
        pojo.setStartTime(Timestamp.from(trip.startedAt.toInstant()));
        pojo.setEndTime(Timestamp.from(trip.finishedAt.toInstant()));
        pojo.setLake(trip.lakeId);
        pojo.setName(trip.name);
        pojo.setType(trip.type);
        pojo.setMode(trip.mode);
        pojo.setOwner(userId);

        tripDao.create(pojo);
    }

    protected UUID getUserId(@CookieParam("token") Cookie cookie) throws NotAuthenticatedException {
        Algorithm algorithmHS = getJwtSecretAlgorithm();

        try {
            if (cookie == null) {
                throw new NotAuthenticatedException();
            }
            DecodedJWT verify = JWT.require(algorithmHS)
                    .withIssuer("fishola-backend")
                    .build()
                    .verify(cookie.getValue());
            String email = verify.getSubject();

            Optional<FisholaUser> user = userDao.findByEmail(email);
            UUID result = user.map(FisholaUser::getId).orElseThrow(NotAuthenticatedException::new);
            return result;
        } catch (JWTVerificationException ve) {
            ve.printStackTrace();
            throw new NotAuthenticatedException();
        }
    }
}
