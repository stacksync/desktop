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
import java.util.Map;

/**
 *
 * @author pheckel
 */
public class UpdateStatusTextRequest implements Request {
    private String status;
    
    public UpdateStatusTextRequest(Map<String, String> statusMap) {        
        String finalStatus = "";
                
        for(Map.Entry<String, String> entry: statusMap.entrySet()){
            if( entry.getValue().length() > 0 ){
                if(finalStatus.length() > 0){
                    finalStatus += "\n";
                } 
                
                finalStatus += entry.getKey() + " - " + entry.getValue();                
            }
        }
        
        if(finalStatus.length() > 0){
            this.status = finalStatus;
        } else{
            this.status = "Everything is up to date";
        }
    }

    public String getStatusText() {
        return status;
    }

    @Override
    public String toString() {
        
        JsonObject obj = new JsonObject();       
        
        obj.addProperty("request", "UpdateStatusTextRequest");
        obj.addProperty("status", this.status);        
        
        return obj.toString();
        
        //String eStatus = status.replace("\"", "\\\"");        
        //return "{\"request\":\"UpdateStatusTextRequest\",\"status\":\"" + eStatus + "\"}";
    }

    @Override
    public Object parseResponse(String responseLine) {
        return null;
    }
}
