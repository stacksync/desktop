package com.stacksync.desktop.gui.shell;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Scanner;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public class OverlayController {
    
    private final Logger logger = Logger.getLogger(OverlayController.class.getName());
    private static final Config config = Config.getInstance();
    private final DatabaseHelper db = DatabaseHelper.getInstance();
    
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
    
    public void initialize(String folderPath) throws OverlayException {
        // connect with overlay library
        boolean connected = this.nativityControl.connect();
        if (!connected){
            throw new OverlayException("Problem connecting to the nativity plugin.");
        }
        logger.info("Connected to nativity plugin");
        
        // Set the folder to filter
        this.nativityControl.setFilterFolder(folderPath);
        
        // Enable overlay
        this.fileIconControl.enableFileIcons();
        logger.info("Enabled file icons");
        
        // Register icons
        this.registerOverlays();
        this.fileIconControl.removeAllFileIcons();
        
        // Draw overlays
        logger.info("Draw initial ovlerays");

        List<CloneFile> dbFiles = db.getFiles(null);

        for (CloneFile dbFile: dbFiles) {
            if (!dbFile.getFile().exists()) {
                continue;
            }
            
            if (dbFile.getSyncStatus() == CloneFile.SyncStatus.UPTODATE) {
                this.drawOverlay(dbFile.getAbsolutePath(), Status.UPTODATE);
            } else if (dbFile.getSyncStatus() == CloneFile.SyncStatus.UNSYNC) {
                this.drawOverlay(dbFile.getAbsolutePath(), Status.UNSYNC);
            } else if (dbFile.getSyncStatus() == CloneFile.SyncStatus.SYNCING) {
                this.drawOverlay(dbFile.getAbsolutePath(), Status.SYNCING);
            }
        }
    }
    
    public void stop() throws OverlayException {
        
        // Unregister overlays
        this.unregisterOverlays();
        
        // Disable overlays
        this.fileIconControl.disableFileIcons();
        
        // Disconnect overlay library
        boolean disconnected = this.nativityControl.disconnect();
        if (!disconnected) {
            throw new OverlayException("Problem disconnecting to the nativity plugin.");
        }
        logger.info("Overlay controller stopped.");
    }
    
    public void refreshFile(File file) { }
    
    public void drawOverlay(String path, Status status) {
        this.fileIconControl.setFileIcon(path, this.iconsIds.get(status));
    }
    
    public void removeOverlay() {
        
    }
    
    private void registerOverlays() {
        String basePath = config.getResDir()+File.separator+Constants.OVERLAY_FOLDER+File.separator;
        
        int uptodateId = fileIconControl.registerIcon(basePath+Constants.OVERLAY_ICNS_UPTODATE);
        this.iconsIds.put(Status.UPTODATE, uptodateId);
        int syncingId = fileIconControl.registerIcon(basePath+Constants.OVERLAY_ICNS_SYNCING);
        this.iconsIds.put(Status.SYNCING, syncingId);
        int unsyncableId = fileIconControl.registerIcon(basePath+Constants.OVERLAY_ICNS_UNSYNCABLE);
        this.iconsIds.put(Status.UNSYNC, unsyncableId);
    }
    
    private void unregisterOverlays() {
        this.fileIconControl.unregisterIcon(this.iconsIds.get(Status.UPTODATE));
        this.fileIconControl.unregisterIcon(this.iconsIds.get(Status.SYNCING));
        this.fileIconControl.unregisterIcon(this.iconsIds.get(Status.UNSYNC));
    }
    
    public static void main(String[] args){
        OverlayController controller = new OverlayController();
        try {
            controller.initialize("/Users/cotes/test/overlay");
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
