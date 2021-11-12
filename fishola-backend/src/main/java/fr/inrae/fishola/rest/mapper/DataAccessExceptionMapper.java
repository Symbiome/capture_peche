package fr.inrae.fishola.rest.mapper;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2021 INRAE - UMR CARRTEL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.jooq.exception.DataAccessException;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class DataAccessExceptionMapper implements ExceptionMapper<DataAccessException> {

    @Inject
    protected Logger log;

    @Override
    public Response toResponse(DataAccessException exception) {
        Response.ResponseBuilder responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
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

        log.warnf("%s thrown: %s", exception.getClass().getName(), entity);

        Response result = responseBuilder.build();
        return result;
    }

}
