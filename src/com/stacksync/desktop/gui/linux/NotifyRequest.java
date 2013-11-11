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

import java.io.File;

/**
 *
 * @author pheckel
 */
public class NotifyRequest implements Request {
    private String summary;
    private String body;
    private File imageFile;

    public NotifyRequest(String summary, String body, File imageFile) {
        this.summary = summary;
        this.body = body;
        this.imageFile = imageFile;
    }

    public String getBody() {
        return body;
    }

    public String getSummary() {
        return summary;
    }

    public File getImageFile() {
        return imageFile;
    }

    @Override
    public String toString() {
        String eSummary = summary.replace("\"", "\\\"");
        String eBody = body.replace("\"", "\\\"");
        
        String eImage = "";
        if(imageFile != null ){
            eImage = imageFile.getAbsolutePath().replace("\"", "\\\"");
        }
        
        return "{\"request\":\"NotifyRequest\",\"summary\":\"" + eSummary + "\",\"body\":\""+eBody+"\",\"image\":\""+eImage+"\"}";
    }

    @Override
    public Object parseResponse(String responseLine) {
        return null;
    }
}
