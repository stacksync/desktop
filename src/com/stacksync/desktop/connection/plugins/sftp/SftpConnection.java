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
package com.stacksync.desktop.connection.plugins.sftp;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.Plugins;
import java.util.ResourceBundle;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Thomas Tschager <dontpanic@tschager.net>
 */
public class SftpConnection implements Connection {
    private String host;
    private String username;
    private String password;
    private String path;
    private String keyPath;
    private Integer port;
    private Boolean keyAuth;
    private String passphrase;
    private ResourceBundle resourceBundle;

    public SftpConnection() {
         resourceBundle = Config.getInstance().getResourceBundle();
    }
    
    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(SftpPluginInfo.ID);
    }
 
    @Override
    public TransferManager createTransferManager() {
        return new SftpTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new SftpConfigPanel(this);
    }
  
    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isKeyAuth() {
        return keyAuth;
    }

    public void setKeyAuth(boolean keyAuth) {
        this.keyAuth = keyAuth;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        host = node.getProperty("host");
        username = node.getProperty("username");
        password = node.getProperty("password");
        path = node.getProperty("path");
        keyAuth = Boolean.parseBoolean(node.getProperty("keyAuth"));
        keyPath = node.getProperty("keyPath");
        passphrase = node.getProperty("passphrase");
        
        if (host == null || username == null || password == null || path == null) {
            throw new ConfigException("SFTP connection properties must at least contain the parameters 'host', 'username', 'password' and 'path'.");
        }
        
        if (keyPath != null && !keyPath.endsWith("/") ){
            keyPath = keyPath + "/";
        }
        
        // Optional
        try { 
            port = Integer.parseInt(node.getProperty("port", "22"));
        } catch (NumberFormatException e) {
            throw new ConfigException("Invalid port number in config exception: "+node.getProperty("port"));
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", SftpPluginInfo.ID);

        node.setProperty("host", host);
        node.setProperty("username", username);
        node.setProperty("password", password);
        node.setProperty("path", path);
        node.setProperty("port", port);
        node.setProperty("keyAuth", keyAuth);
        node.setProperty("keyPath", keyPath);
        node.setProperty("passphrase", passphrase);
    }
    
    @Override
    public String toString() {
        return SftpConnection.class.getSimpleName()
                + "[" + resourceBundle.getString("sftp_host") + "=" + host + ":" + port + 
                ", " + resourceBundle.getString("sftp_username") + "=" + username
                + ", " + resourceBundle.getString("sftp_path") + "=" + path + ", " +
                resourceBundle.getString("sftp_key_auth_name") + "=" + keyAuth
                + ", " + resourceBundle.getString("sftp_key_path_name") + "=" + keyPath + "]";
    }	
}
