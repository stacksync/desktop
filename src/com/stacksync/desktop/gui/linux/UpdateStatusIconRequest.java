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


import com.google.gson.JsonObject;
import com.stacksync.desktop.gui.tray.Tray.StatusIcon;

/**
 *
 * @author pheckel
 */
public class UpdateStatusIconRequest implements Request {
    private StatusIcon status;
    
    public UpdateStatusIconRequest(StatusIcon status) {
        this.status = status;
    }

    public StatusIcon getStatus() {
        return status;
    }

    @Override
    public String toString() {
        JsonObject obj = new JsonObject();       
        
        obj.addProperty("request", "UpdateStatusIconRequest");
        obj.addProperty("status", this.status.toString());        
        
        return obj.toString();
        
        //return "{\"request\":\"UpdateStatusIconRequest\",\"status\":\"" + status + "\"}";
    }

    @Override
    public Object parseResponse(String responseLine) {
        return null;
    }
}
