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
package com.stacksync.desktop.connection.plugins.swift;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.encryption.PasswordCipher;
import com.stacksync.desktop.encryption.PasswordCipherFactory;
import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Plugins;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.swift_comercial.SwiftComercialPluginInfo;
import com.stacksync.desktop.connection.plugins.swift_dev.SwiftDevPluginInfo;
import com.stacksync.desktop.exceptions.ConfigException;
import java.util.ResourceBundle;

/**
 *
 * @author pheckel
 */
public class SwiftConnection implements Connection {
    private final Config config = Config.getInstance();
    // By default we use the DUMMY cipher to avoid problems with
    // old versions.
    private final PasswordCipherFactory.EncryptType encrypType = PasswordCipherFactory.EncryptType.DUMMY;
    
    private String username;
    private String apiKey;
    private String container;
    private String authUrl;
    private String tenant;
    private String user;
    private ResourceBundle resourceBundle;
    
    public SwiftConnection() {
        resourceBundle = config.getResourceBundle();
    }
    
    
    @Override
    public PluginInfo getPluginInfo() {
        
        if (config.isExtendedMode()) {
            return Plugins.get(SwiftDevPluginInfo.ID);
        } else {
            return Plugins.get(SwiftComercialPluginInfo.ID);
        }
        
    }
        
    @Override
    public TransferManager createTransferManager() {
        return new SwiftTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        ConfigPanel panel;
        
        if (config.isExtendedMode()) {
            panel = SwiftConfigPanelFactory.getSwiftConfigPanel(SwiftConfigPanelFactory.DEV, this);
        } else {
            panel = SwiftConfigPanelFactory.getSwiftConfigPanel(SwiftConfigPanelFactory.COMERCIAL, this);
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

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
    
    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        username = node.getProperty("username");
        
        PasswordCipher cipher = PasswordCipherFactory.getPasswordEncrypter(encrypType);
        apiKey = cipher.decrypt(node.getProperty("apikey"));
        
        authUrl = node.getProperty("authurl");

        if (username == null || apiKey == null) {
            throw new ConfigException("Swift connection properties must at least contain the parameters 'username' and 'apikey'.");
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", getPluginInfo().getId());
        node.setProperty("username", username);
        
        PasswordCipher cipher = PasswordCipherFactory.getPasswordEncrypter(encrypType);
        String encryptedApiKey = cipher.encrypt(apiKey);
        node.setProperty("apikey", encryptedApiKey);
        node.setProperty("authurl", authUrl);
    }
    
    @Override
    public String toString() {
        return SwiftConnection.class.getSimpleName()
            + "[" + resourceBundle.getString("swift_username")  + "=" + username +
            ", Auth url=" + authUrl + "]";
    }

    @Override
    public String getPassword() {
        return apiKey;
    }

    @Override
    public String getHost() {
        SwiftTransferManager trans = (SwiftTransferManager) createTransferManager();
        return trans.getStorageIp();
    }
}
