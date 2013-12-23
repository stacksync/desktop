/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.gui.tray;

import com.stacksync.desktop.ApplicationController;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public class TrayEventListenerImpl implements TrayEventListener {

    private ApplicationController controller;
    private final Logger logger = Logger.getLogger(TrayEventListenerImpl.class.getName());
    
    public TrayEventListenerImpl(ApplicationController controller) {
        this.controller = controller;
    }
    
    @Override
    public void trayEventOccurred(TrayEvent event) {
        switch (event.getType()) {
            case OPEN_FOLDER:
                File folder = new File((String) event.getArgs().get(0));
                FileUtil.openFile(folder);
                break;

            /*case PREFERENCES:
                settingsDialog.setVisible(true);
                break;*/

            case WEBSITE:
                FileUtil.browsePage(Constants.APPLICATION_URL);
                break;

            case WEBSITE2:
                FileUtil.browsePage(Constants.APPLICATION_URL2);
                break;
                
            case PAUSE_SYNC:
                this.controller.pauseSync();
                break;
                
            case RESUME_SYNC:
                this.controller.resumeSync();
                break;

            case QUIT:
                this.controller.doShutdownTray();
                break;

            default:
                //checkthis
                logger.warn("Unknown tray event type: " + event);
            // Fressen.
        }
    }
}