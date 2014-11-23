package com.stacksync.desktop.gui.shell;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;
import java.io.File;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public class OverlayController {
    
    private final Logger logger = Logger.getLogger(OverlayController.class.getName());
    
    private NativityControl nativityControl;
    private FileIconControl fileIconControl;
        
    public OverlayController() {
        this.nativityControl = NativityControlUtil.getNativityControl();
        this.fileIconControl = FileIconControlUtil.getFileIconControl(
                nativityControl,
                new FileIconControlCallback() {
                        @Override
                        public int getIconForFile(String path) {
                                return 0;
                        }
                }
        );
    }
    
    public void initialize() throws OverlayException {
        // connect with overlay library
        boolean connected = this.nativityControl.connect();
        if (!connected){
            throw new OverlayException("Problem connecting to the nativity plugin.");
        }
        logger.info("Connected to nativity plugin");
        
        // Set the folder to filter
        
        
        // Enable overlay
        this.fileIconControl.enableFileIcons();
        logger.info("Enabled file icons");
        
        // Register icons
        
        // Draw overlays
    }
    
    public void stop() throws OverlayException {
        
        // Disable overlays
        this.fileIconControl.disableFileIcons();
        
        // Disconnect overlay library
        boolean disconnected = this.nativityControl.disconnect();
        if (!disconnected) {
            throw new OverlayException("Problem disconnecting to the nativity plugin.");
        }
        logger.info("Overlay controller stopped.");
    }
    
    public void refreshFile(File file) {
        
    }
    
    public void drawOverlay() {
        
    }
    
    public void removeOverlay() {
        
    }
    
    private void registerOverlay() {
        
    }
    
    private void unregisterOverlay() {
        
    }
    
}
