package com.stacksync.desktop.index.requests;

import java.io.File;
import org.apache.log4j.Logger;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.sharing.WorkspaceController;

public class RenameIndexWorkspaceRequest extends IndexRequest {
    
    private final Logger logger = Logger.getLogger(RenameIndexWorkspaceRequest.class.getName());
    
    private CloneFile dbFromFile;
    private File fromFile;
    private File toFile;

    public RenameIndexWorkspaceRequest(File fromFile, File toFile) {
        super();
        this.fromFile = fromFile;
        this.toFile = toFile;
        
    }
    
    public RenameIndexWorkspaceRequest(CloneFile dbFromFile, File toFile) {
        this(dbFromFile.getFile(), toFile);
        this.dbFromFile = dbFromFile;
    }
    
    @Override
    public void process() {
        logger.info("Indexer: Updating renamed workspace "+fromFile.getName()+" TO "+toFile.getName()+"");
        
        if (!dbFromFile.isFolder()) {
            logger.error("Indexer: file must be a folder -> NOT INDEXING!");
            return;
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);
        
        CloneWorkspace local = db.getWorkspace(dbFromFile.getWorkspace().getId());
        
        CloneWorkspace remote = local.clone();
        remote.setName(toFile.getName());
        remote.setPathWorkspace("/"+toFile.getName());
        
        WorkspaceController.getInstance().changeWorkspaceName(local, remote, false);

        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }
    
}