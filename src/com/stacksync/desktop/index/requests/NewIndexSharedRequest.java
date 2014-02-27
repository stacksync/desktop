package com.stacksync.desktop.index.requests;

import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.index.Indexer;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.util.Date;
import org.apache.log4j.Logger;

public class NewIndexSharedRequest extends SingleRootIndexRequest {
    
    private final Logger logger = Logger.getLogger(NewIndexRequest.class.getName());
    
    private File file;
    private long checksum;

    public NewIndexSharedRequest(Folder root, File file, long checksum) {
        super(root);
  
        this.file = file;        
        this.checksum = checksum;
    }

    @Override
    public void process() {                
        logger.info("Indexer: Indexing new file "+file+" ...");
        
        // ignore file        
        if (FileUtil.checkIgnoreFile(root, file)) {
            logger.info("#ndexer: Ignore file "+file+" ...");
            return;
        }
        
        // File vanished
        if (!file.exists()) {
            logger.warn("Indexer: Error indexing file "+file+": File does NOT exist. Ignoring.");            
            return;
        }
        
        // Find file in DB
        CloneFile dbFile = db.getFileOrFolder(root, file);
        if(dbFile != null){
            if(dbFile.getChecksum() == checksum){
                logger.warn("Indexer: Error already Indexed this version. Ignoring.");
                return;
            }
            
            if(dbFile.isFolder()){
                logger.warn("Indexer: Error already NewIndexRequest this folder. Ignoring.");
                return;
            }
        }
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);
        
        // Create DB entry
        CloneFile newVersion = addNewVersion();                      

        File parentFile = FileUtil.getCanonicalFile(file.getParentFile());
        newVersion.setParent(db.getFolder(root, parentFile));
        
        String folderName = file.getName();
        String[] info = folderName.split("_");
        if (info.length != 3) {
            // Throw exception or queue as a new index request.
        }

        String workspaceId = info[1];
        CloneWorkspace workspace = db.getWorkspaces().get(workspaceId);
        if (workspace == null) {
            // Throw exception or queue as a new index request.
        }
        
        newVersion.setWorkspace(workspace);
        
        // This will check if the file is inside a folder that isn't created.
        if (newVersion.getParent() == null && !newVersion.getPath().equals("/")) {
            Indexer.getInstance().queueNewIndex(root, file, null, checksum);
            return;
        }
        
        newVersion.setFolder(file.isDirectory());
        newVersion.setSize(file.length());
        
        newVersion.setLastModified(new Date(file.lastModified()));
        newVersion.setSyncStatus(CloneFile.SyncStatus.LOCAL);
        newVersion.merge();
        
        // Process folder and files differently
        processFolder(newVersion);
        
        renameSharedFolder();
        
        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }

    private CloneFile addNewVersion() {
        CloneFile newVersion = new CloneFile(root, file);
        
        newVersion.setVersion(1);
        newVersion.setStatus(CloneFile.Status.NEW);        
        
        return newVersion;
    }
    
    private void processFolder(CloneFile cf) {        
        // Add rest of the DB stuff 
        cf.setChecksum(0);
        
        if (FileUtil.checkIllegalName(cf.getName())
                || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))){
            cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
        } else {
            cf.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        }
        cf.merge();
        
        // Not necessary yet. Shared folders don't contain files.
        // Analyze file tree (if directory) -- RECURSIVELY!!
        /*logger.info("Indexer: Indexing CHILDREN OF "+file+" ...");
        for (File child: file.listFiles()) {
            // Ignore .ignore files
            if (FileUtil.checkIgnoreFile(root, child)) {
                continue; 
            }
            
            // Do it!
            logger.info("Indexer: Parent: "+file+" / CHILD "+child+" ...");
            Indexer.getInstance().queueNewIndex(root, child, null, -1);
        }*/
    }
    
    private void renameSharedFolder() {
        
        String folderName = file.getName();
        String[] info = folderName.split("_");
        File newFolder = new File(file.getParentFile()+File.separator+info[2]);
        
        file.renameTo(newFolder);
    }
    
    @Override
    public String toString() {
        return NewIndexSharedRequest.class.getSimpleName() + "[" + "file=" + file + "]";
    }    
    
}
