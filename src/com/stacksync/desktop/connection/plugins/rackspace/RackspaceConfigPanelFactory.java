/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.connection.plugins.rackspace;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.rackspace_comercial.RackspaceComercialConfigPanel;
import com.stacksync.desktop.connection.plugins.rackspace_dev.RackspaceDevConfigPanel;

/**
 *
 * @author cotes
 */
public class RackspaceConfigPanelFactory {
    
    public static final String DEV = "dev";
    public static final String COMERCIAL = "comercial";
    //Default values for comercial wizard
    public static final String AUTH_URL = "AUTH_SERVER_URL";
    public static final String CONTAINER = "stacksync";
    
    public static ConfigPanel getRackspaceConfigPanel(String type, RackspaceConnection connection) {
        
        ConfigPanel panel = null;
        if (type.equals(DEV)) {
            panel = new RackspaceDevConfigPanel(connection);
        } else if (type.equals(COMERCIAL)) {
            panel = new RackspaceComercialConfigPanel(connection);
        }
        
        return panel;
    }
    
}
