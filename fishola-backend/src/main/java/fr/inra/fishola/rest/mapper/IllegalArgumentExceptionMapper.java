package fr.inra.fishola.rest.mapper;

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        Map<String, String> entity = new LinkedHashMap<>();
        if (StringUtils.isNotEmpty(exception.getMessage())) {
            entity.put("error", exception.getMessage());
        }
        Throwable cause = exception.getCause();
        if (cause != null) {
            entity.put("cause", cause.getClass().getName() + ": " + cause.getMessage());
        }
        if (!entity.isEmpty()) {
            responseBuilder.entity(entity);
        }
        Response result = responseBuilder.build();
        return result;
    }

}
