/*
 * Syncany, www.syncany.org
 * Copyright (C) 2012 Maurus Cuelenaere<mcuelenaere@gmail.com>
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
package com.stacksync.desktop.connection.plugins.dropbox;

import java.util.ResourceBundle;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.PluginInfo;

/**
 * @author Maurus Cuelenaere <mcuelenaere@gmail.com>
 */
public class DropboxPluginInfo extends PluginInfo {
    public static final String ID = "dropbox";

    private ResourceBundle resourceBundle = Config.getInstance().getResourceBundle();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return resourceBundle.getString("dropbox_plugin_name");
    }

    @Override
    public Integer[] getVersion() {
        return new Integer[] {0, 1};
    }

    @Override
    public String getDescripton() {
        return resourceBundle.getString("dropbox_plugin_description");
    }

    @Override
    public Connection createConnection() {
        return new DropboxConnection();
    }
}
