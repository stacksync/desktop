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
package com.stacksync.desktop.connection.plugins.picasa;

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
 */
public class PicasaConnection implements Connection {
    private String username;
    private String password;
    private String albumId;
    private ResourceBundle resourceBundle;

    public PicasaConnection() {
        resourceBundle = Config.getInstance().getResourceBundle();
    }

    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(PicasaPluginInfo.ID);
    }

    @Override
    public TransferManager createTransferManager() {
        return new PicasaTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new PicasaConfigPanel(this);
    }
  
    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String album) {
        this.albumId = album;
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
        username = node.getProperty("username");
        password = node.getProperty("password");
        albumId = node.getProperty("album");

        if (username == null || password == null || albumId == null) {
            throw new ConfigException("Picasa connection properties must at least contain the parameters 'username', 'password' and 'album'.");
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", getPluginInfo().getId());

        node.setProperty("username", username);
        node.setProperty("password", password);
        node.setProperty("album", albumId);
    }
    
    @Override
    public String toString() {
        return PicasaConnection.class.getSimpleName()
        + "[" + resourceBundle.getString("picasa_username") + "=" + username + ", " + resourceBundle.getString("picasa_album")+ "=" + albumId + "]";
    }	

    @Override
    public String getHost() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
