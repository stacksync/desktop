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

import com.stacksync.desktop.connection.plugins.Connection;
import com.stacksync.desktop.connection.plugins.PluginInfo;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class BoxPluginInfo extends PluginInfo {
    public static final String ID = "box";
    
    @Override
    public String getId() {
        return ID;
    }    
    
    @Override
    public String getName() {
        return "Box.net";
    }

    @Override
    public Integer[] getVersion() {
        return new Integer[] { 0, 1 };
    }

    @Override
    public String getDescripton() {
        return "Uses a Box.net folder as data repository.";
    }

    @Override
    public Connection createConnection() {
        return new BoxConnection();
    }
}
