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
package com.stacksync.desktop.config;

import com.stacksync.desktop.exceptions.ConfigException;
import java.util.Date;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Plugins;
/**
 *
 * @author Philipp C. Heckel
 */
public final class Repository implements Configurable {
    
    /**
     * Clonebox divides bigger files in chunks. This value defines the kilobytes (KB) of
     * how big one (unencrypted) chunk might become (1024 = 1 MB).
     */
    public static final int DEFAULT_CHUNK_SIZE = 512;

    private Connection connection;

    /**
     * Maximum size of each (unencrypted) chunk in bytes. After encrypting
     * each chunk, their size may vary slightly.
     *
     * <p>Note: As of the current design, this value should not be changed. Doing
     * so means losing access to repositories with the previous chunk size!
     */
    private int chunkSize;
    
    private Date lastUpdate;   
    private boolean connected;

    // New
    public Repository() {
        // Fressen
        connection = null; // Loaded or set dynamically!
        
        lastUpdate = null;
        connected = false;
        chunkSize = DEFAULT_CHUNK_SIZE;
    }
 
    /**
     * Returns the chunk size in kilobytes
     * @return
     */
    public int getChunkSize() {
        return chunkSize;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public boolean isConnected() {
        return connected;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        if (node == null) {
            throw new ConfigException("Missing repository.");
        }

        try {            
            chunkSize = node.getInteger("chunksize", DEFAULT_CHUNK_SIZE);

            // Connection
            ConfigNode connectionNode = node.findChildByXPath("connection");
            if (connectionNode == null) {
                throw new ConfigException("No connection found in repository");
            }

            PluginInfo connectionPlugin = Plugins.get(connectionNode.getAttribute("type"));
            if (connectionPlugin == null) {
                throw new ConfigException("Unknown repository plugin '"+connectionNode.getAttribute("type")+"' in repository.");
            }

            connection = connectionPlugin.createConnection();
            connection.load(connectionNode);

        } catch (Exception e) {
            throw new ConfigException("Unable to load repository: "+node+", error: "+e, e);
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("chunksize", chunkSize);

        connection.save(node.findOrCreateChildByXpath("connection", "connection"));
    }
}
