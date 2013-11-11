/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.exceptions;

public class StorageUnauthorizeException extends StorageException {
    public StorageUnauthorizeException() {
    }

    public StorageUnauthorizeException(String message) {
        super(message);
    }

    public StorageUnauthorizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageUnauthorizeException(Throwable cause) {
        super(cause);
    }
}
