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

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jooq.exception.DataAccessException;

import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class DataAccessExceptionMapper implements ExceptionMapper<DataAccessException> {

    /**
     * Message unique renvoyé au client. Le détail d'une erreur de base de données
     * (requête, noms de colonnes, contraintes, extrait du SQL fautif) renseigne un
     * attaquant sur le schéma et sur ce que sa charge a effectivement atteint : il
     * reste dans les journaux du serveur, il ne sort pas dans la réponse HTTP.
     */
    protected static final String GENERIC_MESSAGE = "Erreur d'accès aux données";

    @Inject
    protected Logger log;

    @Override
    public Response toResponse(DataAccessException exception) {
        log.error("DataAccessException thrown", exception);

        Map<String, String> entity = new LinkedHashMap<>();
        entity.put("error", GENERIC_MESSAGE);

        Response result = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(entity)
                .build();
        return result;
    }

}
