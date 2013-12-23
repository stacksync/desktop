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
package com.stacksync.desktop.connection.plugins.ftp;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.Plugins;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpConnection implements Connection {
    private String host;
    private String username;
    private String password;
    private String path;
    private int port;

    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(FtpPluginInfo.ID);
    }
 
    @Override
    public TransferManager createTransferManager() {
        return new FtpTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new FtpConfigPanel(this);
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

    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        host = node.getProperty("host");
        username = node.getProperty("username");
        password = node.getProperty("password");
        path = node.getProperty("path");
        
        if (host == null || username == null || password == null || path == null) {
            throw new ConfigException("FTP connection properties must at least contain the parameters 'host', 'username', 'password' and 'path'.");
        }
        // Add trailing "/"
        if ( !path.endsWith("/") ) {
            path = path + "/";
        }
        
        // Optional
        try { 
            port = Integer.parseInt(node.getProperty("port", "21"));
        } catch (NumberFormatException e) {
            throw new ConfigException("Invalid port number in config exception: "+node.getProperty("port"));
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", FtpPluginInfo.ID);

        node.setProperty("host", host);
        node.setProperty("username", username);
        node.setProperty("password", password);
        node.setProperty("path", path);
        node.setProperty("port", port);
    }
    
    @Override
    public String toString() {
        return FtpConnection.class.getSimpleName()
        + "[host=" + host + ":" + port + ", username=" + username + ", path=" + path + "]";
    }	
}
