package fr.inra.fishola.exceptions;

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
