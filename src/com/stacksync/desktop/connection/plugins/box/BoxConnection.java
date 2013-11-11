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
package com.stacksync.desktop.connection.plugins.box;

import com.stacksync.desktop.connection.plugins.ConfigPanel;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.Plugins;
import cn.com.believer.songyuanframework.openapi.storage.box.constant.BoxConstant;
import java.util.ResourceBundle;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class BoxConnection implements Connection {
    private String apiKey;
    private String ticket = null;
    private String token = null;    
    private String folderId;
    private ResourceBundle resourceBundle;
    
    private String loginStatus = BoxConstant.STATUS_NOT_LOGGED_IN;

    public BoxConnection() {
        resourceBundle = Config.getInstance().getResourceBundle();
    }

    @Override
    public PluginInfo getPluginInfo() {
        return Plugins.get(BoxPluginInfo.ID);
    }

    @Override
    public TransferManager createTransferManager() {
        return new BoxTransferManager(this);
    }

    @Override
    public ConfigPanel createConfigPanel() {
        return new BoxConfigPanel(this);
    }

    public String getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(String loginStatus) {
        this.loginStatus = loginStatus;
    }
    
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }        

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }   

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }        

    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Mandatory
        apiKey = node.getProperty("apikey");
        ticket = node.getProperty("ticket");
        token = node.getProperty("token");
        folderId = node.getProperty("folderid");

        if (apiKey == null || ticket == null || token == null || folderId == null) {
            throw new ConfigException("Box.net connection properties must at least contain the parameters 'apikey', 'ticket', 'token' and 'folderid'.");
        }        
    }

    @Override
    public void save(ConfigNode node) {
        node.setAttribute("type", BoxPluginInfo.ID);

        node.setProperty("apikey", apiKey);
        node.setProperty("ticket", ticket);
        node.setProperty("token", token);
        node.setProperty("folderid", folderId);        
    }
    
    @Override
    public String toString() {
        return BoxConnection.class.getSimpleName()
        + "[" + resourceBundle.getString("box_apiKey") + "=" + apiKey + 
        ", " + resourceBundle.getString("box_folderId") + "=" + folderId + "]";
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
