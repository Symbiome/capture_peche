package fr.inra.fishola.rest;

import fr.inra.fishola.AbstractFisholaRuntimeException;

import javax.ws.rs.core.Response;

/**
 * @author Arnaud Thimel (Code Lutin)
 */
public class AccessDeniedException extends AbstractFisholaRuntimeException {

    protected static final String DEFAULT_MESSAGE = "Access denied";

    protected AccessDeniedException(String message) {
        super(message);
    }

    public static void check(boolean expression) throws AccessDeniedException {
        check(expression, DEFAULT_MESSAGE);
    }

    public static void check(boolean expression, String message) throws AccessDeniedException {
        if ( ! expression) {
            throwNew(message);
        }
    }

    public static void throwNew(String message) throws AccessDeniedException {
        throw new AccessDeniedException(message);
    }

    public static void throwNew() throws AccessDeniedException {
        throwNew(DEFAULT_MESSAGE);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.FORBIDDEN;
    }

}
