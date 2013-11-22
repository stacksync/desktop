/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.exceptions;

public class TrayException extends Exception {
    public TrayException() {
    }

    public TrayException(String message) {
        super(message);
    }

    public TrayException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrayException(Throwable cause) {
        super(cause);
    }
}
