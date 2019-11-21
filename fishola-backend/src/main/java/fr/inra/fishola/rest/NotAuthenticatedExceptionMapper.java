package fr.inra.fishola.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotAuthenticatedExceptionMapper implements ExceptionMapper<NotAuthenticatedException> {

    @Override
    public Response toResponse(NotAuthenticatedException exception) {
        Response result = Response.status(Response.Status.UNAUTHORIZED).build();
        return result;
    }

}
