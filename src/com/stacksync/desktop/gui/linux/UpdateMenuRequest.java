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

import java.util.ArrayList;
import java.util.List;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;

/**
 *
 * @author pheckel
 */
public class UpdateMenuRequest implements Request {
    private List<ProfileProxy> profiles;

    public UpdateMenuRequest(Profile profile) {
        this.profiles = new ArrayList<ProfileProxy>();
        
        if (profile == null) {
            return;
        }

        ProfileProxy profileProxy = new ProfileProxy();	    
        profileProxy.setName(profile.getName());

        List<FolderProxy> folderProxies = new ArrayList<FolderProxy>();

        Folder folder = profile.getFolder();
        
        if (folder != null) {
            folderProxies.add(new FolderProxy(folder.getLocalFile()));
        }

        profileProxy.setFolders(folderProxies);
        this.profiles.add(profileProxy);
    }

    public List<ProfileProxy> getProfiles() {
        return profiles;
    }

    @Override
    public String toString() {        
        return "{\"request\":\"UpdateMenuRequest\",\"profiles\":" + profiles + "}";
    }

    @Override
    public Object parseResponse(String responseLine) {
        return null;
    }
}
