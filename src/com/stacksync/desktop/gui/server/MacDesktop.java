package com.stacksync.desktop.gui.server;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.gui.shell.OverlayController;
import com.stacksync.desktop.gui.shell.OverlayException;
import java.io.File;
import org.apache.log4j.Logger;

public class MacDesktop extends Desktop {
    private final Logger logger = Logger.getLogger(MacDesktop.class.getName());
    private static final Config config = Config.getInstance();
       
    private OverlayController controller;
    private boolean initialized;
 
    protected MacDesktop() {
        logger.info("Creating desktop integration ...");
        this.controller = new OverlayController();
    }

    @Override
    public void start(boolean startDemonOnly) {
        
        if (this.initialized) {
            logger.info("Mac desktop already started.");
            return;
        }
        
        logger.info("Starting desktop services (daemon: " + startDemonOnly + ") ...");
        try {
            Folder folder = config.getProfile().getFolder();
            this.controller.initialize(folder.getLocalFile().getPath());
            this.initialized = true;
        } catch (OverlayException ex) {
            logger.error(ex.getMessage());
            this.initialized = false;
        }
    }

    @Override
    public void touch(File file) {}
    
    @Override
    public void touch(String filepath, CloneFile.SyncStatus status) {
        if (this.initialized) {
            this.controller.drawOverlay(filepath, status);
        }
    }
    
    @Override
    public void untouch(String filepath) {
        if (this.initialized) {
            this.controller.removeOverlay(filepath);
        }
    }
    
    @Override
    public void stop(boolean startDemonOnly) {
        if (!this.initialized) {
            return;
        }
        
        try {
            this.controller.stop();
        } catch (OverlayException ex) {
            logger.error(ex.getMessage());
        }

    }
}
