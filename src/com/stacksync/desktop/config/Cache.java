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

import java.io.File;
import java.io.IOException;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.exceptions.CacheException;
import com.stacksync.desktop.exceptions.ConfigException;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Cache implements Configurable {
    /* private static final Config config = Config.getInstance();
     * 
     * WARNING: Do NOT add 'Config' as a static final here. 
     *          Since this class is created in the Config constructor, 
     *          Config.getInstance() will return NULL.
     */
    
    /**
     * 1 = Cache folder
     * 2 = Chunk file name (defined by {@link CloneChunk#getFileName()})
     */
    private static String CHUNK_FORMAT = "%1$s/%2$s";

    /**
     * 1 = Cache folder
     * 2 = Account ID
     * 3 = File name (defined by {@link CloneFile#getFileName()})
     */
    //private static String FILE_FORMAT = "%1$s/ac-%2$s-%3$s";

    private int size;
    private File folder;
    
    public Cache() {
        this.folder = null;
        this.size = Constants.DEFAULT_CACHE_SIZE;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public File getFolder() {
        return folder;
    }

    public int getSize() {
        return size;
    }

    public File getCacheChunk(CloneChunk chunk) {
        return new File(String.format(CHUNK_FORMAT,
            folder.getAbsoluteFile(),
            chunk.getName())
        );
    }

    public File createTempFile() throws CacheException {
        return createTempFile("temp");
    }

    public File createTempFile(String name) throws CacheException {
       try {
           return File.createTempFile(
                String.format("ac-%1$s-temp-%s-", Config.getInstance().getDeviceName(), name),
                ".tmp",
                folder);
       }
       catch (IOException e) {
           throw new CacheException("Unable to create temporary file in cache.", e);
       }
   }

    @Override
    public void load(ConfigNode node) throws ConfigException {
        // Defaults
        folder = new File(Config.getInstance().getConfDir() + File.separator + Constants.CONFIG_CACHE_DIRNAME);
        size = Constants.DEFAULT_CACHE_SIZE;

        if (node == null) {
            return;
        }

        // Override?!
        String sCacheDir = node.getProperty("folder");

        // Cache directory (default): Create default if it doesn't exist
        if (sCacheDir == null) {
            Config.getInstance().createDirectory(folder);
        } else { // Cache directory is set: Do not create if it doesn't exist
            folder = new File(sCacheDir);
            if(!folder.exists()){
                Config.getInstance().createDirectory(folder);
            }
        }
        
        if (!folder.exists() || !folder.isDirectory() || !folder.canRead() || !folder.canWrite()) {
            throw new ConfigException("Given cache directory '"+folder+"' does not exist or is not read/writable.");
        }

        // Size
        Integer cacheSize = node.getInteger("size");

        if (cacheSize != null) {
            size = cacheSize;
        }
    }

    @Override
    public void save(ConfigNode node) {
        node.setProperty("folder", folder.getAbsolutePath());
        node.setProperty("size", size);
    }
    
    @Override
    public String toString() {
        return Cache.class.getSimpleName() + "[size=" + size + ", folder=" + folder.getAbsolutePath() + "]";
    }    
}
