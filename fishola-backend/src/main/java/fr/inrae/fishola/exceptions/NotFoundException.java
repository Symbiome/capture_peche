package fr.inrae.fishola.exceptions;

/*-
 * #%L
 * Fishola :: Backend
 * %%
 * Copyright (C) 2019 - 2020 INRAE - UMR CARRTEL
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.core.Response;

/**
 * @author Arnaud Thimel (Code Lutin)
 */
public class NotFoundException extends AbstractFisholaRuntimeException {

    private static final Log log = LogFactory.getLog(NotFoundException.class);

    protected static final String DEFAULT_MESSAGE = "Not found";

    protected NotFoundException(String message) {
        super(message);
    }

    public static void check(boolean expression) throws AccessDeniedException {
        check(expression, DEFAULT_MESSAGE);
    }

    public static void check(boolean expression, String message) throws AccessDeniedException {
        if (!expression) {
            throwNew(message);
        }
    }

    public static void throwNew(String message) throws AccessDeniedException {
        if (log.isDebugEnabled()) {
            log.warn("NotFoundException: " + message);
        }
        throw new AccessDeniedException(message);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.NOT_FOUND;
    }

}
