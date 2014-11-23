package com.stacksync.desktop.gui.server;

import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.gui.shell.OverlayController;
import com.stacksync.desktop.gui.shell.OverlayException;
import java.io.File;
import org.apache.log4j.Logger;

public class MacDesktop extends Desktop {
    private final Logger logger = Logger.getLogger(MacDesktop.class.getName());
    private static final Config config = Config.getInstance();
       
    private OverlayController controller;
 
    protected MacDesktop() {
        logger.info("Creating desktop integration ...");
        this.controller = new OverlayController();
    }

    @Override
    public void start(boolean startDemonOnly) {
        logger.info("Starting desktop services (daemon: " + startDemonOnly + ") ...");
        try {
            Folder folder = config.getProfile().getFolder();
            this.controller.initialize(folder.getLocalFile().getPath());
        } catch (OverlayException ex) {
            logger.error(ex.getMessage());
        }
    }

    @Override
    public void touch(File file) {
        this.controller.refreshFile(file);
    }
    
    @Override
    public void stop(boolean startDemonOnly) {
        try {
            this.controller.stop();
        } catch (OverlayException ex) {
            logger.error(ex.getMessage());
        }

    }
}
