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
package com.stacksync.desktop.config.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Configurable;
import com.stacksync.desktop.exceptions.ConfigException;
import org.w3c.dom.Node;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Profiles implements Configurable {
    /* private static final Config config = Config.getInstance();
     * 
     * WARNING: Do NOT add 'Config' as a static final here. 
     *          Since this class is created in the Config constructor, 
     *          Config.getInstance() will return NULL.
     */
    
    public static String tagName() { return "profiles"; }
    public static String xpath() { return "profiles"; }
    
    private TreeMap<Integer, Profile> profiles;

    public Profiles() {
        this.profiles = new TreeMap<Integer, Profile>();
    }

    public List<Profile> list() {
        return new ArrayList<Profile>(profiles.values());
    }

    public Profile get(int profileId) {
        return profiles.get(profileId);
    }
    
    public Profile get(String name) {
        for (Profile p : profiles.values()) {
            if (name.equals(p.getName())) {
                return p;
            }
        }
	
        return null;
    }

    public synchronized Profile add(Profile profile) {
        // Cross-reference: Repo!
        //if (!config.getRepositories().contains(profile.getRepository().getId()))
        //     config.getRepositories().add(profile.getRepository());

        // Profile
        Integer newId = (profiles.isEmpty()) ? 1 : profiles.lastKey()+1;

        profile.setId(newId);
        profiles.put(newId, profile);

        return profile;
    }

    public synchronized Profile remove(int profileId) {
        Profile profile = profiles.get(profileId);

        if (profile == null) {
            return null;
        }

        profiles.remove(profileId);
        return profile;
    }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        for (ConfigNode profileNode : node.findChildrenByXpath("profile")) {
            Profile profile = new Profile();
            profile.load(profileNode);
            
            profiles.put(profile.getId(), profile);
        }        
    }

    @Override
    public void save(ConfigNode node) {
        // Remove deleted profiles from DOM
        for (Node profileNode : node.findChildren(Profile.tagName())) {
            Integer id = Integer.parseInt(new ConfigNode(profileNode).getAttribute("id"));

            if (!profiles.containsKey(id)) {
                node.getNode().removeChild(profileNode);
            }
        }

        // Add or modify in DOM
        for (Profile profile : profiles.values()) {
            ConfigNode profileNode = node.findOrCreateChildByXpath(Profile.xpath(profile.getId()), Profile.tagName());
            profile.save(profileNode);
        }
    }

}