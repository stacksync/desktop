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
package com.stacksync.desktop.test;

import java.io.File;
import java.util.Map;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.connection.plugins.PluginInfo;
import com.stacksync.desktop.connection.plugins.Plugins;
import com.stacksync.desktop.connection.plugins.TransferManager;
import com.stacksync.desktop.connection.plugins.rackspace.RackspaceConnection;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.exceptions.LocalFileNotFoundException;
import com.stacksync.desktop.exceptions.StorageConnectException;
import com.stacksync.desktop.exceptions.StorageException;
import com.stacksync.desktop.repository.files.RemoteFile;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class TestPlugin {
    public static void main(String[] args) throws StorageConnectException, LocalFileNotFoundException, StorageException, ConfigException {
        Config.getInstance().load();
        
        PluginInfo plugin = Plugins.get("box");
        RackspaceConnection c = (RackspaceConnection) plugin.createConnection();
        
        c.setApiKey("...");
        
        TransferManager tm = c.createTransferManager();
        
        tm.connect();
        //tm.upload(new File("/etc/hosts"), new RemoteFile("hosts"));
        tm.download(new RemoteFile("hosts"), new File("/home/pheckel/hosts"));
        tm.delete(new RemoteFile("hosts"));
        Map<String, RemoteFile> list = tm.list();
        
        for (RemoteFile f : list.values()) {
            System.out.println(f);

        }

    }
}
