package fr.inra.fishola.rest.mapper;

import fr.inra.fishola.exceptions.AbstractFisholaRuntimeException;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class FisholaExceptionMapper implements ExceptionMapper<AbstractFisholaRuntimeException> {

    @Override
    public Response toResponse(AbstractFisholaRuntimeException exception) {
        Response.ResponseBuilder responseBuilder = Response.status(exception.getStatus());
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
