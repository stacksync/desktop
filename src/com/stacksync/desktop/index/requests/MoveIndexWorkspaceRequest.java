package com.stacksync.desktop.index.requests;

import java.io.File;
import java.util.Date;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.util.FileUtil;

public class MoveIndexWorkspaceRequest extends IndexRequest {
    
    private final Logger logger = Logger.getLogger(MoveIndexWorkspaceRequest.class.getName());
    
    private CloneFile dbFromFile;
    
    private Folder fromRoot;
    private File fromFile;
    
    private Folder toRoot;
    private File toFile;    

    public MoveIndexWorkspaceRequest(Folder fromRoot, File fromFile, Folder toRoot, File toFile) {
        super();
         
        this.fromRoot = fromRoot;
        this.fromFile = fromFile;
        
        this.toRoot = toRoot;
        this.toFile = toFile;
        
    }
    
    public MoveIndexWorkspaceRequest(CloneFile dbFromFile, Folder toRoot, File toFile) {
        this(dbFromFile.getRoot(), dbFromFile.getFile(), toRoot, toFile);
        this.dbFromFile = dbFromFile;
    }
    
    @Override
    public void process() {
        logger.info("Indexer: Updating moved/renamed workspace "+fromFile.getAbsolutePath()+" TO "+toFile.getAbsolutePath()+"");
        
        if (!dbFromFile.isFolder()) {
            logger.error("Indexer: file must be a folder -> NOT INDEXING!");
            return;
        }

        // Parent 
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);
        CloneFile cToParentFolder = db.getFolder(toRoot, new File(absToParentFolder));
        
        // Check if the movement is between different workspaces
        /*CloneFile fromFileParent = dbFromFile.getParent();
        CloneWorkspace fromWorkspace = (fromFileParent == null) ? dbFromFile.getWorkspace() : fromFileParent.getWorkspace();
        CloneWorkspace toWorkspace = (cToParentFolder == null) ? db.getDefaultWorkspace() : cToParentFolder.getWorkspace();
        if (isWorkspaceChanged(fromWorkspace, toWorkspace)) {
            logger.info("Item workspace changed. Queueing delete and new requests.");
            Indexer.getInstance().queueDeleted(fromRoot, fromFile);
            Indexer.getInstance().queueNewIndex(toRoot, toFile, null, -1);
            return;
        }*/
                
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);

        // File found in DB.
        CloneFile dbToFile = (CloneFile) dbFromFile.clone();

        // Updated changes
        dbToFile.setRoot(toRoot);
        dbToFile.setLastModified(new Date(toFile.lastModified()));
        dbToFile.setName(toFile.getName());
        dbToFile.setSize((toFile.isDirectory()) ? 0 : toFile.length());
        dbToFile.setVersion(dbToFile.getVersion()+1);
        dbToFile.setStatus(Status.RENAMED);
        dbToFile.setSyncStatus(CloneFile.SyncStatus.UPTODATE);

        dbToFile.setParent(cToParentFolder);
        dbToFile.setMimetype(FileUtil.getMimeType(dbToFile.getFile()));
        dbToFile.generatePath();
        
        dbToFile.setServerUploadedAck(true);
        dbToFile.setServerUploadedTime(new Date(toFile.lastModified()));
        dbToFile.merge();
	    
        // Notify file manager
        desktop.touch(dbToFile.getFile());
	    
        processFolder(dbToFile);
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }
    
    private void processFolder(CloneFile cf) {
        
        /*
        // 4. If previous version was UNSYNC
        if (dbFromFile.getSyncStatus() == CloneFile.SyncStatus.UNSYNC){

            // Search for the last synced version to create the next version
            CloneFile lastSyncedVersion = cf.getLastSyncedVersion();
            if (lastSyncedVersion == null) {
                cf.setVersion(1);
                cf.setStatus(Status.NEW);
            } else {
                cf.setVersion(lastSyncedVersion.getVersion()+1);
            }

            cf.merge();

            // Clean unsynced versions from DB
            cf.deleteHigherVersion();
        }
        
        if (FileUtil.checkIllegalName(cf.getName())
                || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))){
            logger.info("This folder contains illegal characters.");
            cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
            cf.merge();
            return;
        }
        
        cf.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        cf.merge();*/
        
    }
    
    private boolean isWorkspaceChanged(CloneWorkspace fromWorkspace, CloneWorkspace toWorkspace) {
        
        boolean workspaceChanged = true;
        
        if (fromWorkspace.getId().equals(toWorkspace.getId())) {
            workspaceChanged = false;
        }
        
        return workspaceChanged;
    }
}