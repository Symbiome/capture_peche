package fr.inrae.fishola.exceptions;

import javax.ws.rs.core.Response;

public abstract class AbstractFisholaRuntimeException extends RuntimeException {

    public AbstractFisholaRuntimeException(String message) {
        super(message);
    }

    public AbstractFisholaRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract Response.Status getStatus();

}
