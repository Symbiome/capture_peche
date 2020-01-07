package fr.inra.fishola.rest;

import fr.inra.fishola.AbstractFisholaRuntimeException;

import javax.ws.rs.core.Response;

public class NotAuthenticatedException extends AbstractFisholaRuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.UNAUTHORIZED;
    }

}
