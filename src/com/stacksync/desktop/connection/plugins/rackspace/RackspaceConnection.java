/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.connection.plugins.rackspace;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.Plugins;
import java.util.ResourceBundle;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.connection.plugins.rackspace_comercial.RackspaceComercialPluginInfo;
import com.stacksync.desktop.connection.plugins.rackspace_dev.RackspaceDevPluginInfo;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 * @author pheckel
 */
public class RackspaceConnection implements Connection {
    private final Config config = Config.getInstance();
    
    private String username;
    private String apiKey;
    private String container;
    private String authUrl;
    private ResourceBundle resourceBundle;

    public RackspaceConnection() {
        resourceBundle = config.getResourceBundle();
    }
    
    
    @Override
    public PluginInfo getPluginInfo() {
        
        if (config.isExtendedMode()) {
            return Plugins.get(RackspaceDevPluginInfo.ID);
        } else {
            return Plugins.get(RackspaceComercialPluginInfo.ID);
        }
        
    }
        
    @Override
    public TransferManager createTransferManager() {
        return new RackspaceTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        //return new RackspaceConfigPanel(this);
        ConfigPanel panel;
        
        if (config.isExtendedMode()) {
            panel = RackspaceConfigPanelFactory.getRackspaceConfigPanel(RackspaceConfigPanelFactory.DEV, this);
        } else {
            panel = RackspaceConfigPanelFactory.getRackspaceConfigPanel(RackspaceConfigPanelFactory.COMERCIAL, this);
        }
        
        return panel;
    }
    
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }
    
    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        username = node.getProperty("username");
        apiKey = node.getProperty("apikey");
        container = node.getProperty("container");
        // CCG
        authUrl = node.getProperty("authurl");

        if (username == null || apiKey == null || container == null) {
            throw new ConfigException("Rackspace connection properties must at least contain the parameters 'username', 'apikey' and 'container'.");
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", getPluginInfo().getId());
        node.setProperty("username", username);
        node.setProperty("apikey", apiKey);
        node.setProperty("container", container);
        // CCG
        node.setProperty("authurl", authUrl);
    }
    
    @Override
    public String toString() {
        return RackspaceConnection.class.getSimpleName()
            + "[" + resourceBundle.getString("rackspace_username")  + "=" + username +
            ", " + resourceBundle.getString("rackspace_container") + "=" + container + 
            ", Auth url=" + authUrl + "]";
    }

    @Override
    public String getPassword() {
        return apiKey;
    }

    @Override
    public String getHost() {
        RackspaceTransferManager trans = (RackspaceTransferManager) createTransferManager();
        return trans.getStorageIp();
    }
}
