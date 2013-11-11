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
package com.stacksync.desktop.connection.plugins.imap;

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
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class ImapConnection implements Connection {
    public enum Security { NONE, STARTTLS, SSL };

    private String host;
    private int port;
    private String username;
    private String password;
    private String folder;
    private Security security;
    private ResourceBundle resourceBundle;

    public ImapConnection() {
        resourceBundle = Config.getInstance().getResourceBundle();
    }

    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(ImapPluginInfo.ID);
    }

    @Override
    public TransferManager createTransferManager() {
        return new ImapTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new ImapConfigPanel(this);
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String imapFolder) {
        this.folder = imapFolder;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String imapHost) {
        this.host = imapHost;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String imapPassword) {
        this.password = imapPassword;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int imapPort) {
        this.port = imapPort;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }
   
    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String imapUsername) {
        this.username = imapUsername;
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        host = node.getProperty("host");
        username = node.getProperty("username");
        password = node.getProperty("password");
        folder = node.getProperty("folder");

        if (host == null || username == null || password == null || folder == null) {
            throw new ConfigException("IMAP connection properties must at least contain the parameters 'host', 'username', 'password' and 'folder'.");
        }

        // Optional
        String strSecurity = node.getProperty("security");

        if (strSecurity == null) security = Security.NONE;
        else if ("none".equals(strSecurity.toLowerCase())) security = Security.NONE;
        else if ("starttls".equals(strSecurity.toLowerCase())) security = Security.STARTTLS;
        else if ("ssl".equals(strSecurity.toLowerCase())) security = Security.SSL;
        else {
            throw new ConfigException("Invalid value given for security property in IMAP connection.");
        }

        port = node.getInteger("port", 143);
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", ImapPluginInfo.ID);
        
        node.setProperty("host", host);
        node.setProperty("username", username);
        node.setProperty("password", password);
        node.setProperty("folder", folder);
        node.setProperty("security", security);
        node.setProperty("port", port);
    }
    
    @Override
    public String toString() {
        return ImapConnection.class.getSimpleName()
            + "["+ resourceBundle.getString("imap_host") + "=" + host + ":" + port + 
            ", " + resourceBundle.getString("imap_username") + "=" + username + ", =" + resourceBundle.getString("imap_folder") 
            + folder + "]";
    }	   
}
