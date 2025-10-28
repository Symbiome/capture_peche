package fr.inrae.fishola.exceptions;

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

import jakarta.ws.rs.core.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Arnaud Thimel (Code Lutin)
 */
public class NotFoundException extends AbstractFisholaRuntimeException {

    private static final Log log = LogFactory.getLog(NotFoundException.class);

    protected static final String DEFAULT_MESSAGE = "Not found";

    protected NotFoundException(String message) {
        super(message);
    }

    public static void check(boolean expression) throws NotFoundException {
        check(expression, DEFAULT_MESSAGE);
    }

    public static void check(boolean expression, String message) throws NotFoundException {
        if (!expression) {
            throwNew(message);
        }
    }

    public static void throwNew(String message) throws NotFoundException {
        if (log.isDebugEnabled()) {
            log.warn("NotFoundException: " + message);
        }
        throw new NotFoundException(message);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.NOT_FOUND;
    }

}
