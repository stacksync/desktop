package com.stacksync.desktop.index.requests;

import com.stacksync.desktop.config.Folder;
import java.io.File;
import java.util.Date;
import org.apache.log4j.Logger;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.syncserver.Server;
import com.stacksync.desktop.util.FileUtil;

public class RenameIndexWorkspaceRequest extends IndexRequest {
    
    private final Logger logger = Logger.getLogger(RenameIndexWorkspaceRequest.class.getName());
    
    private CloneFile dbFromFile;
    private File fromFile;
    private File toFile;
    private Folder root;

    public RenameIndexWorkspaceRequest(File fromFile, File toFile) {
        super();
        this.fromFile = fromFile;
        this.toFile = toFile;
        
    }
    
    public RenameIndexWorkspaceRequest(CloneFile dbFromFile, File toFile) {
        this(dbFromFile.getFile(), toFile);
        this.dbFromFile = dbFromFile;
        this.root = dbFromFile.getRoot();
    }
    
    @Override
    public void process() {
        logger.info("Indexer: Updating renamed workspace "+fromFile.getName()+" TO "+toFile.getName()+"");
        
        if (!dbFromFile.isFolder()) {
            logger.error("Indexer: file must be a folder -> NOT INDEXING!");
            return;
        }

        // Parent 
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);
        CloneFile cToParentFolder = db.getFolder(root, new File(absToParentFolder));
                
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);

        // File found in DB.
        CloneFile dbToFile = (CloneFile) dbFromFile.clone();

        // Updated changes
        dbToFile.setRoot(root);
        dbToFile.setLastModified(new Date(toFile.lastModified()));
        dbToFile.setName(toFile.getName());
        dbToFile.setSize((toFile.isDirectory()) ? 0 : toFile.length());
        dbToFile.setVersion(dbToFile.getVersion()+1);
        dbToFile.setStatus(Status.RENAMED);
        dbToFile.setSyncStatus(CloneFile.SyncStatus.UPTODATE);

        dbToFile.setParent(cToParentFolder);
        dbToFile.setMimetype(FileUtil.getMimeType(dbToFile.getFile()));
        dbToFile.generatePath();
        
        dbToFile.setServerUploadedAck(false);
        dbToFile.setServerUploadedTime(new Date(toFile.lastModified()));
        
        // Update workspace
        CloneWorkspace workspace = dbToFile.getWorkspace();
        workspace.setName(dbToFile.getName());
        workspace.merge();
        
        // Save new version to DB
        dbToFile.merge();
	    
        // Notify file manager
        desktop.touch(dbToFile.getFile());
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }
    
}