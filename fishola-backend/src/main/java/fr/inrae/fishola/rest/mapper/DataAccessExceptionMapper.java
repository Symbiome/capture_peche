package fr.inrae.fishola.rest.mapper;

import org.apache.commons.lang3.StringUtils;
import org.jooq.exception.DataAccessException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class DataAccessExceptionMapper implements ExceptionMapper<DataAccessException> {

    @Override
    public Response toResponse(DataAccessException exception) {
        Response.ResponseBuilder responseBuilder = Response.status(500);
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
