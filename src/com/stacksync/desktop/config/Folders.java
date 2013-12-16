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

import com.stacksync.desktop.config.profile.Profile;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Folders implements Configurable {
    public static final String TAG_NAME = "folders";

    private Profile profile;
    private TreeMap<String, Folder> folders;

    public Folders(Profile profile) {
        this.profile = profile;
        this.folders = new TreeMap<String, Folder>();
    }

    public List<Folder> list() {
        return new ArrayList<Folder>(folders.values());
    }

    public Folder get(String remoteId) {
        return folders.get(remoteId);
    }
    
    public synchronized boolean update(Folder folder) {
        if (get(folder.getRemoteId()) == null) {
            return false;
        }
         
         folder.setProfile(profile);
         folders.put(folder.getRemoteId(), folder);
         
         return true;
    }

    public synchronized boolean add(Folder folder) {
        if (get(folder.getRemoteId()) != null) {
            return false;
        }

        folder.setProfile(profile);
        folders.put(folder.getRemoteId(), folder);
        
        return true;
    }


    @Override
    public void load(ConfigNode node) throws ConfigException {
        if (node == null) {
            throw new ConfigException("Unable to load folders.");
        }

        List<ConfigNode> folderList = node.findChildrenByXpath("folder");

        if (folderList == null) {
            throw new ConfigException("Folder mappings are missing in repository.");
        }

        for (ConfigNode folderNode : folderList) {
            Folder folder = new Folder();
            folder.load(folderNode);

            add(folder);
        }
    }

    @Override
    public void save(ConfigNode node) {
        for (Folder folder : folders.values()) {
            folder.save(node.findOrCreateChildByXpath("folder[remote='"+folder.getRemoteId()+"']", "folder"));
        }
    }

    public void clear() {
        folders.clear();
    }
}
