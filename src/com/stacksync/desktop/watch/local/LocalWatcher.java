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
package com.stacksync.desktop.watch.local;

import java.io.File;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.Environment.OperatingSystem;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.util.FileUtil;

/**
 *
 * @author oubou68, pheckel
 */
public abstract class LocalWatcher {
    protected final Logger logger = Logger.getLogger(LocalWatcher.class.getName());
    protected static final Environment env = Environment.getInstance();
    protected static LocalWatcher instance;
    protected Config config;
    protected Indexer indexer;

    public LocalWatcher() {
        initDependencies();
        logger.info("Creating watcher ...");        
    }

    private void initDependencies() {
        config = Config.getInstance();
        indexer = Indexer.getInstance();
    }

    public void queueCheckFile(Folder root, File file) {
        // Exclude ".ignore*" files from everything
        if (FileUtil.checkIgnoreFile(root, file)) {
            logger.debug("Watcher: Ignoring file "+file.getAbsolutePath());
            return;
        }
        
        // File vanished!
        if (!file.exists()) {
            logger.warn("Watcher: File "+file+" vanished. IGNORING.");            
            return;
        }

        // Add to queue    
        logger.info("Watcher: Checking new/modified file "+file);
        indexer.queueChecked(root, file);
    }

    public void queueMoveFile(Folder fromRoot, File fromFile, Folder toRoot, File toFile) {
        // Exclude ".ignore*" files from everything
        if (FileUtil.checkIgnoreFile(fromRoot, fromFile) || FileUtil.checkIgnoreFile(toRoot, toFile)) {            
            logger.info("Watcher: Ignoring file "+fromFile.getAbsolutePath());
            return;
        }
        
        // File vanished!
        if (!toFile.exists()) {
            logger.warn("Watcher: File "+toFile+" vanished. IGNORING.");
            return;
        }

        // Add to queue   
        logger.info("Watcher: Moving file "+fromFile+" TO "+toFile+"");     
        indexer.queueMoved(fromRoot, fromFile, toRoot, toFile);
    }

    public void queueDeleteFile(Folder root, File file) {
        // Exclude ".ignore*" files from everything
        if (FileUtil.checkIgnoreFile(root, file)) {
            logger.info("Watcher: Ignoring file "+file.getAbsolutePath());
            return;
        }

        // Add to queue
        logger.info("Watcher: Deleted file "+file+"");        
        indexer.queueDeleted(root, file);
    }

    public static synchronized LocalWatcher getInstance() {
        if (instance != null) {
            return instance;
        }

        if (env.getOperatingSystem() == OperatingSystem.Linux
            || env.getOperatingSystem() == OperatingSystem.Windows
            || env.getOperatingSystem() == OperatingSystem.Mac) {
            
            instance = new CommonLocalWatcher(); 
            return instance;
        }

        throw new RuntimeException("Your operating system is currently not supported: " + System.getProperty("os.name"));
    }

    public abstract void start();

    public abstract void stop();

    public abstract void watch(Profile profile);

    public abstract void unwatch(Profile profile);
}
