package fr.inrae.fishola.exceptions;

import javax.ws.rs.core.Response;

public class NotAuthenticatedException extends AbstractFisholaRuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }

    public NotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.UNAUTHORIZED;
    }

}
