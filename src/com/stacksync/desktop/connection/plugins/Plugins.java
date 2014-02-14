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
package com.stacksync.desktop.connection.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.util.StringUtil;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Plugins {
    private static final Logger logger = Logger.getLogger(Plugins.class.getName());
    
    private static final Map<String, PluginInfo> plugins = new TreeMap<String, PluginInfo>();    
    private static boolean loaded = false;
    private static Thread asyncLoadThread = null;
    
    public static void loadAsync(){

        if(asyncLoadThread == null){
            // Pre-load the plugins
            asyncLoadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("Preloading Plugins Start");	
                    Plugins.load();
                    logger.info("Preloading Plugins End");	
                }
            }, "PreloadPlugins");
            asyncLoadThread.start();
        }
    }
    
    public static void waitForAsyncLoaded() throws InterruptedException {
       if(asyncLoadThread!=null){
           asyncLoadThread.join();
       }
    }

    private static void load() {
        if (loaded) {
            return;
        }
        
        loaded = true;                
         
        List<String> pluginsToLoad = new ArrayList<String>();
        //plugins.add("box"); 
        //plugins.add("dropbox");
        pluginsToLoad.add("ftp");
        //plugins.add("gs");
        //plugins.add("imap");
        //plugins.add("local");
        //plugins.add("picasa");
        //plugins.add("pop3_smtp");
        pluginsToLoad.add("rackspace_dev");
        pluginsToLoad.add("s3");
        //pluginsToLoad.add("samba");
        pluginsToLoad.add("sftp");
        //pluginsToLoad.add("webdav");        
        
        // Only load the needed plugins        
        for(String pl: pluginsToLoad){
            loadPlugin(pl);
        }        
    }

    public static Collection<PluginInfo> list() {
        loadAsync();
        try{
            waitForAsyncLoaded();
        } catch(InterruptedException e){
            // Swallow this Exception, not sure what to do here anyways.
        }
        return plugins.values();
    }

    /**
     * Loads the plugin by a given ID.
     *
     * <p>Does not call the list() method to boost performance.
     * 
     * @param pluginId
     * @return
     */
    public static PluginInfo get(String pluginId) {
        // If already loaded, get from list
        if (plugins.containsKey(pluginId)) {
            return plugins.get(pluginId);
        }

        // Try to load via name
        loadPlugin(pluginId);

        if (plugins.containsKey(pluginId)) {
            return plugins.get(pluginId);
        }

        // Not found!
        return null;
    }

    private static void loadPlugin(String pluginId) {
        String className = String.format(Constants.PLUGIN_FQCN_PATTERN, pluginId, StringUtil.toCamelCase(pluginId));
        loadPlugin(pluginId, className);
    }

    private static void loadPlugin(String pluginId, String className) {
        // Already loaded
        if (plugins.containsKey(pluginId)) {
            return;
        }

        // Try to load!
        try {
            Class pluginInfoClass = Class.forName(className);
            PluginInfo pluginInfo = (PluginInfo) pluginInfoClass.newInstance();

            plugins.put(pluginId, pluginInfo);
        }
        catch (Exception ex) {
            logger.warn("Could not load plugin : "+className, ex);
        }
    }
}
