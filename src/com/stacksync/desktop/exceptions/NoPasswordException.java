package com.stacksync.desktop.exceptions;

/**
 *
 * @author cotes
 */
public class NoPasswordException extends Exception {

    public NoPasswordException(Throwable cause) {
        super(cause);
    }

    public NoPasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoPasswordException(String message) {
        super(message);
    }

    public NoPasswordException() {
    }

}
