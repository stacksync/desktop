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
package com.stacksync.desktop.connection.plugins.samba;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.Plugins;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 * @author Philipp C. Heckel
 */
public class SambaConnection implements Connection {
    
    private String domain;
    private String user;
    private String password;

    private String path;

    public SambaConnection() { }
    
    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(SambaPluginInfo.ID);
    }

    @Override
    public TransferManager createTransferManager() {
        return new SambaTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new SambaConfigPanel(this);
    }

    public String getRoot() {
        return path;
    }
    

    public void setRoot(String root) {
        if (!root.endsWith("/")) {
            root = root+"/";
        }

        this.path = root;
    }
    
    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        String strFolder = node.getProperty("root");

        if (strFolder == null) {
            throw new ConfigException("Samba connection must at least contain the 'root'.");
        }
        
        path = strFolder;	
        
        domain = node.getProperty("domain");
        user = node.getProperty("user");
        password = node.getProperty("password");
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", SambaPluginInfo.ID);
        node.setProperty("root", path);
        
        node.setProperty("domain", domain);
        node.setProperty("user", user);
        node.setProperty("password", password);
    }
    
    @Override
    public String toString() {
        return SambaConnection.class.getSimpleName()
        + "[domain=" + domain + ", user=" + user + ", password=" + password + ", path=" + path + "]";
    }	      

    @Override
    public String getUsername() {
        return user;
    }
    
    public String getDomain(){
        return domain;
    }

    @Override
    public String getPassword() {
        return password;
    }

    void setDomain(String domain) {
        this.domain = domain;
    }

    void setUsername(String user) {
        this.user = user;
    }

    void setPassword(String password) {
        this.password = password;                
    }

    @Override
    public String getHost() {
        SambaTransferManager trans = (SambaTransferManager) createTransferManager();
        return trans.getStorageIp();
    }
}
