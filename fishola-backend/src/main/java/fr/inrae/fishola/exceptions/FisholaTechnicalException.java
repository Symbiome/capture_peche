package fr.inrae.fishola.exceptions;

import javax.ws.rs.core.Response;

public class FisholaTechnicalException extends AbstractFisholaRuntimeException {

    public FisholaTechnicalException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

}
