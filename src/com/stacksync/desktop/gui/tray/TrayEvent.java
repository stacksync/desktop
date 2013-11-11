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
package com.stacksync.desktop.gui.tray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Philipp C. Heckel
 */
public class TrayEvent implements Serializable {

    public enum EventType {
        /*
         * NOTE: If you change this, remember to adjust these settings in the
         *       Linux 'native.py' script!
         */
        OPEN_FOLDER,
        PREFERENCES,
        QUIT,
        DONATE,
        WEBSITE,
        WEBSITE2
    };
    
    private EventType type;
    private List<String> args;

    public TrayEvent(EventType type) {
        this.type = type;
        this.args = new ArrayList<String>();
    }

    public TrayEvent(EventType type, String... args) {
        this.type = type;
        this.args = Arrays.asList(args);
    }

    public EventType getType() {
        return type;
    }

    public List<String> getArgs() {
        return args;
    }
}
