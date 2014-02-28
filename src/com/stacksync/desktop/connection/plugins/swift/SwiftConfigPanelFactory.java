/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.connection.plugins.swift;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.swift_comercial.SwiftComercialConfigPanel;
import com.stacksync.desktop.connection.plugins.swift_dev.SwiftDevConfigPanel;

/**
 *
 * @author cotes
 */
public class SwiftConfigPanelFactory {
    
    public static final String DEV = "dev";
    public static final String COMERCIAL = "comercial";
    //Default values for comercial wizard
    public static final String AUTH_URL = "AUTH_SERVER_URL";
    public static final String CONTAINER = "stacksync";
    
    public static ConfigPanel getSwiftConfigPanel(String type, SwiftConnection connection) {
        
        ConfigPanel panel = null;
        if (type.equals(DEV)) {
            panel = new SwiftDevConfigPanel(connection);
        } else if (type.equals(COMERCIAL)) {
            panel = new SwiftComercialConfigPanel(connection);
        }
        
        return panel;
    }
    
}
