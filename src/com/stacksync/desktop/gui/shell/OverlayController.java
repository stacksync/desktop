package com.stacksync.desktop.gui.shell;

import com.liferay.nativity.control.NativityControl;
import com.liferay.nativity.control.NativityControlUtil;
import com.liferay.nativity.modules.fileicon.FileIconControl;
import com.liferay.nativity.modules.fileicon.FileIconControlCallback;
import com.liferay.nativity.modules.fileicon.FileIconControlUtil;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneItemVersion.SyncStatus;
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
    
    private NativityControl nativityControl;
    private FileIconControl fileIconControl;
    private EnumMap<SyncStatus, Integer> iconsIds;
        
    public OverlayController() {
        
        this.iconsIds = new EnumMap<SyncStatus, Integer>(SyncStatus.class);
        
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

        List<CloneItem> dbFiles = db.getFiles();

        for (CloneItem dbFile: dbFiles) {
            if (!dbFile.getFile().exists()) {
                continue;
            }
            
            CloneItemVersion latestVersion = dbFile.getLatestVersion();
            
            if (latestVersion.getSyncStatus() == SyncStatus.UPTODATE) {
                this.drawOverlay(dbFile.getAbsolutePath(), SyncStatus.UPTODATE);
            } else if (latestVersion.getSyncStatus() == SyncStatus.UNSYNC) {
                this.drawOverlay(dbFile.getAbsolutePath(), SyncStatus.UNSYNC);
            } else if (latestVersion.getSyncStatus() == SyncStatus.SYNCING) {
                this.drawOverlay(dbFile.getAbsolutePath(), SyncStatus.SYNCING);
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
    
    public void drawOverlay(String path, SyncStatus status) {
        this.fileIconControl.setFileIcon(path, this.iconsIds.get(status));
    }
    
    public void removeOverlay(String path) {
        this.fileIconControl.removeFileIcon(path);
    }
    
    private void registerOverlays() {
        String basePath = config.getResDir()+File.separator+Constants.OVERLAY_FOLDER+File.separator;
        
        int uptodateId = fileIconControl.registerIcon(basePath+Constants.OVERLAY_ICNS_UPTODATE);
        this.iconsIds.put(SyncStatus.UPTODATE, uptodateId);
        int syncingId = fileIconControl.registerIcon(basePath+Constants.OVERLAY_ICNS_SYNCING);
        this.iconsIds.put(SyncStatus.SYNCING, syncingId);
        int unsyncableId = fileIconControl.registerIcon(basePath+Constants.OVERLAY_ICNS_UNSYNCABLE);
        this.iconsIds.put(SyncStatus.UNSYNC, unsyncableId);
    }
    
    private void unregisterOverlays() {
        this.fileIconControl.unregisterIcon(this.iconsIds.get(SyncStatus.UPTODATE));
        this.fileIconControl.unregisterIcon(this.iconsIds.get(SyncStatus.SYNCING));
        this.fileIconControl.unregisterIcon(this.iconsIds.get(SyncStatus.UNSYNC));
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
        controller.drawOverlay("/Users/cotes/test/overlay/a", SyncStatus.UPTODATE);
        controller.drawOverlay("/Users/cotes/test/overlay/a/b", SyncStatus.UPTODATE);
    }
}
