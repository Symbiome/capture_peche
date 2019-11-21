package fr.inra.fishola.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.UUID;

@Path("/api/v1/security")
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource {

    @GET
    @Path("/login")
    public Response login() {

        // TODO AThimel 21/11/2019 Real key
        Algorithm algorithmHS = Algorithm.HMAC512("v3ry 53cr37 k3y");

//        iss issuer : qui a émis le token
//        sub subject : identifiant unique métier
//        aud audience : fishola mobile ?
//        exp date d'expritration
//        nbf not before
//        iat issued at
//        jti identifiant unique : uuid

        Date now = new Date();
        String token = JWT.create()
                .withIssuer("fishola-backend")
                .withSubject("this-is-my-login")
                .withIssuedAt(now)
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithmHS);

        NewCookie cookie = new NewCookie("token", token);
        // FIXME AThimel 21/11/2019 Secure + HTTPOnly
        Response result = Response.ok().cookie(cookie).build();
        return result;

    }

}
