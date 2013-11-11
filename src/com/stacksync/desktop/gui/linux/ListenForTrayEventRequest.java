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
package com.stacksync.desktop.gui.linux;

import com.stacksync.desktop.gui.tray.TrayEvent;
import com.stacksync.desktop.gui.tray.TrayEvent.EventType;

/**
 *
 * @author pheckel
 */
public class ListenForTrayEventRequest implements Request {
    @Override
    public String toString() {
        return "{\"request\":\"ListenForTrayEventRequest\"}";
    }

    @Override
    public Object parseResponse(String responseLine) {
        try {
            String[] line = responseLine.split("\t");

            // Event
            String cmd = line[0];      
            EventType e = TrayEvent.EventType.valueOf(cmd);

            // Arguments
            if (line.length == 1) {
                return new TrayEvent(e, new String[]{ });
            } else {
                String[] args = new String[line.length-1];
                System.arraycopy(line, 1, args, 0, args.length);

                return new TrayEvent(e, args);        
            }
        } catch (Exception e) {
            return null;
        }        
    }
}
