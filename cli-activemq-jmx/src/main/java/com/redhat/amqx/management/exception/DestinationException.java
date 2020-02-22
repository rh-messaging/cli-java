package com.redhat.amqx.management.exception;

/**
 * Custom destination related exceptions.
 */
@SuppressWarnings("serial")
public class DestinationException extends Exception {
    public DestinationException() {
        super();
    }

    public DestinationException(String message) {
        super(message);
    }

    public DestinationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DestinationException(Throwable cause) {
        super(cause);
    }

    protected DestinationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
