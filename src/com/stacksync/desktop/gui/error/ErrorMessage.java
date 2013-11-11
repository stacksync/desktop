package com.stacksync.desktop.gui.error;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *
 * @author gguerrero
 */
public class ErrorMessage {
    
    public static void showMessage(Component parent, String title, String message){
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String args[]) {        
        ErrorMessage.showMessage(null, "Error", "Esto es una prueba.");
    }
}
