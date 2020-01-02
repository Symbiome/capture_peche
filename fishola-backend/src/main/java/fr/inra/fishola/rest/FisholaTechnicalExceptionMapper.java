package fr.inra.fishola.rest;

import fr.inra.fishola.FisholaTechnicalException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class FisholaTechnicalExceptionMapper implements ExceptionMapper<FisholaTechnicalException> {

    @Override
    public Response toResponse(FisholaTechnicalException exception) {
        Response result = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        return result;
    }

}
