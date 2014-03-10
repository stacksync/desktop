package com.stacksync.desktop.index.requests;

import java.io.File;
import org.apache.log4j.Logger;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.sharing.WorkspaceController;
import com.stacksync.desktop.util.FileUtil;

public class MoveIndexWorkspaceRequest extends IndexRequest {
    
    private final Logger logger = Logger.getLogger(MoveIndexWorkspaceRequest.class.getName());
    
    private CloneFile dbFromFile;
    private File fromFile;
    private Folder toRoot;
    private File toFile;    

    public MoveIndexWorkspaceRequest(Folder fromRoot, File fromFile, Folder toRoot, File toFile) {
        super();
         
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
        logger.info("Indexer: Updating moved workspace "+fromFile.getAbsolutePath()+" TO "+toFile.getAbsolutePath()+"");
        
        if (!dbFromFile.isFolder()) {
            logger.error("Indexer: file must be a folder -> NOT INDEXING!");
            return;
        }

        // Parent 
        String absToParentFolder = FileUtil.getAbsoluteParentDirectory(toFile);
        CloneFile cToParentFolder = db.getFolder(toRoot, new File(absToParentFolder));
                
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);
        
        CloneWorkspace local = db.getWorkspace(dbFromFile.getWorkspace().getId());
        
        CloneWorkspace remote = local.clone();
        remote.setName(toFile.getName());
        if (cToParentFolder != null) {
            String path = cToParentFolder.getPath();
            if (!path.endsWith("/")) {
                path += "/";
            }
            path += cToParentFolder.getName()+"/"+toFile.getName();
            remote.setPathWorkspace(path);
            remote.setParentId(cToParentFolder.getId());
        } else {
            remote.setPathWorkspace("/"+toFile.getName());
            remote.setParentId(null);
        }
        
        WorkspaceController.getInstance().changeWorkspaceParent(local, remote, false);
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }
}