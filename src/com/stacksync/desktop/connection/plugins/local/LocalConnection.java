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
package com.stacksync.desktop.connection.plugins.local;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.Plugins;
import java.io.File;
import java.util.ResourceBundle;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 * @author Philipp C. Heckel
 */
public class LocalConnection implements Connection {
    private File folder;
    private int throttleKbps;    
    private ResourceBundle resourceBundle;

    public LocalConnection() {
        resourceBundle = Config.getInstance().getResourceBundle();
    }

    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(LocalPluginInfo.ID);
    }

    @Override
    public TransferManager createTransferManager() {
        return new LocalTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new LocalConfigPanel(this);
    }

    public File getFolder() {
        return folder;
    }

    public int getThrottleKbps() {
        return throttleKbps;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public void setThrottleKbps(int throttleKbps) {
        this.throttleKbps = throttleKbps;
    }        

    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        String strFolder = node.getProperty("folder");

        if (strFolder == null) {
            throw new ConfigException("Local connection must at least contain a 'folder'.");
        }

        folder = new File(strFolder);

        // Optional
        throttleKbps = node.getInteger("throttle-kbps", 0);
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", "local");
        node.setProperty("folder", folder);
        // Do NOT save throttle!
    }
    
    @Override
    public String toString() {
        return LocalConnection.class.getSimpleName()
            + "[" + resourceBundle.getString("local_folder") + "=" + folder + "]";
    }	      

    @Override
    public String getUsername() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getHost() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
