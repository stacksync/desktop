package com.stacksync.desktop.gui.shell;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;
import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Scanner;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public class OverlayController {
    
    private final Logger logger = Logger.getLogger(OverlayController.class.getName());
    
    public enum Status { UNKNOWN, LOCAL, SYNCING, UPTODATE, CONFLICT, REMOTE, UNSYNC };
    
    private NativityControl nativityControl;
    private FileIconControl fileIconControl;
    private EnumMap<Status, Integer> iconsIds;
        
    public OverlayController() {
        
        this.iconsIds = new EnumMap<Status, Integer>(Status.class);
        
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
        this.nativityControl.setFilterFolder("/Users/cotes/test/overlay");
        
        // Enable overlay
        this.fileIconControl.enableFileIcons();
        logger.info("Enabled file icons");
        
        // Register icons
        int uptodateId = fileIconControl.registerIcon("/Users/cotes/test/icons/ok.icns");
        this.iconsIds.put(Status.UPTODATE, uptodateId);
        int syncingId = fileIconControl.registerIcon("/Users/cotes/test/icons/sync.icns");
        this.iconsIds.put(Status.SYNCING, syncingId);
        
        // Draw overlays
        this.fileIconControl.setFileIcon("/Users/cotes/test/overlay/a", syncingId);
        this.fileIconControl.setFileIcon("/Users/cotes/test/overlay/a/b", syncingId);
    }
    
    public void stop() throws OverlayException {
        
        // Unregister overlays
        this.fileIconControl.unregisterIcon(this.iconsIds.get(Status.UPTODATE));
        this.fileIconControl.unregisterIcon(this.iconsIds.get(Status.SYNCING));
        
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
    
    public void drawOverlay(String path, Status status) {
        this.fileIconControl.setFileIcon(path, this.iconsIds.get(status));
    }
    
    public void removeOverlay() {
        
    }
    
    private void registerOverlay() {
        
    }
    
    private void unregisterOverlay() {
        
    }
    
    public static void main(String[] args){
        OverlayController controller = new OverlayController();
        try {
            controller.initialize();
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            updateOverlays(controller);
            scanner.nextLine();
            controller.stop();
        } catch (OverlayException ex) { }
    }
    
    public static void updateOverlays(OverlayController controller){
        controller.drawOverlay("/Users/cotes/test/overlay/a", Status.UPTODATE);
        controller.drawOverlay("/Users/cotes/test/overlay/a/b", Status.UPTODATE);
    }
}
